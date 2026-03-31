package com.crm.service;

import com.crm.dto.request.LeadCreateRequest;
import com.crm.dto.request.LeadUpdateRequest;
import com.crm.dto.request.StageChangeRequest;
import com.crm.dto.response.LeadDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.BadRequestException;
import com.crm.exception.ConflictException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Lead;
import com.crm.model.entity.User;
import com.crm.model.enums.LeadStage;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.UserRepository;
import com.crm.util.InputSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    // Defines the strict forward progression order for lead stages
    private static final Map<LeadStage, LeadStage> NEXT_STAGE = Map.of(
            LeadStage.NEW, LeadStage.CONTACTED,
            LeadStage.CONTACTED, LeadStage.QUALIFIED,
            LeadStage.QUALIFIED, LeadStage.PROPOSAL,
            LeadStage.PROPOSAL, LeadStage.WON
    );

    // Maps each stage to the stage name that must come before it
    private static final Map<LeadStage, String> REQUIRED_PREVIOUS = Map.of(
            LeadStage.CONTACTED, "NEW",
            LeadStage.QUALIFIED, "CONTACTED",
            LeadStage.PROPOSAL, "QUALIFIED",
            LeadStage.WON, "PROPOSAL"
    );

    public LeadService(LeadRepository leadRepository,
                       CustomerRepository customerRepository,
                       UserRepository userRepository) {
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LeadDTO createLead(LeadCreateRequest request, User currentUser) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Lead lead = Lead.builder()
                .customer(customer)
                .title(InputSanitizer.sanitize(request.getTitle()))
                .value(request.getValue())
                .expectedCloseDate(request.getExpectedCloseDate())
                .probability(request.getProbability() != null ? request.getProbability() : 0)
                .owner(currentUser)
                .build();

        Lead saved = leadRepository.save(lead);
        return LeadDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> getLeads(Pageable pageable, String stageFilter, User currentUser) {
        LeadStage stage = parseStageFilter(stageFilter);

        switch (currentUser.getRole()) {
            case ADMIN:
                return stage != null
                        ? leadRepository.findByStage(stage, pageable).map(LeadDTO::fromEntity)
                        : leadRepository.findAll(pageable).map(LeadDTO::fromEntity);
            case MANAGER:
                List<UUID> visibleUserIds = getVisibleUserIds(currentUser);
                return stage != null
                        ? leadRepository.findByOwnerIdInAndStage(visibleUserIds, stage, pageable).map(LeadDTO::fromEntity)
                        : leadRepository.findByOwnerIdIn(visibleUserIds, pageable).map(LeadDTO::fromEntity);
            case SALES_REP:
            default:
                return stage != null
                        ? leadRepository.findByOwnerIdAndStage(currentUser.getId(), stage, pageable).map(LeadDTO::fromEntity)
                        : leadRepository.findByOwnerId(currentUser.getId(), pageable).map(LeadDTO::fromEntity);
        }
    }

    @Transactional(readOnly = true)
    public LeadDTO getLeadById(UUID id, User currentUser) {
        Lead lead = findLeadOrThrow(id);
        checkAccess(lead, currentUser);
        return LeadDTO.fromEntity(lead);
    }

    @Transactional
    public LeadDTO updateLead(UUID id, LeadUpdateRequest request, User currentUser) {
        Lead lead = findLeadOrThrow(id);
        checkAccess(lead, currentUser);

        if (request.getVersion() == null || !request.getVersion().equals(lead.getVersion())) {
            throw new ConflictException("The resource was modified by another user. Please retry.");
        }

        if (request.getTitle() != null) {
            lead.setTitle(InputSanitizer.sanitize(request.getTitle()));
        }
        if (request.getValue() != null) {
            lead.setValue(request.getValue());
        }
        if (request.getExpectedCloseDate() != null) {
            lead.setExpectedCloseDate(request.getExpectedCloseDate());
        }
        if (request.getProbability() != null) {
            lead.setProbability(request.getProbability());
        }

        Lead saved = leadRepository.save(lead);
        return LeadDTO.fromEntity(saved);
    }

    @Transactional
    public LeadDTO changeStage(UUID id, StageChangeRequest request, User currentUser) {
        Lead lead = findLeadOrThrow(id);
        checkAccess(lead, currentUser);

        LeadStage currentStage = lead.getStage();
        LeadStage newStage = request.getNewStage();

        validateStageTransition(currentStage, newStage, request, currentUser);

        // WON requires value > 0
        if (newStage == LeadStage.WON && (lead.getValue() == null || lead.getValue().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BadRequestException("Deal value must be greater than zero to mark as WON");
        }

        lead.setStage(newStage);

        if (newStage == LeadStage.LOST) {
            lead.setLostReason(InputSanitizer.sanitize(request.getLostReason()));
        }

        Lead saved = leadRepository.save(lead);
        return LeadDTO.fromEntity(saved);
    }

    private void validateStageTransition(LeadStage current, LeadStage target,
                                         StageChangeRequest request, User currentUser) {
        if (current == target) {
            throw new BadRequestException("Lead is already in stage " + current.name());
        }

        // LOST is allowed from any stage but requires a reason
        if (target == LeadStage.LOST) {
            if (request.getLostReason() == null || request.getLostReason().isBlank()) {
                throw new BadRequestException("Lost reason is required when marking a lead as LOST");
            }
            return;
        }

        // Reopening from WON or LOST requires MANAGER or ADMIN
        if (current == LeadStage.WON || current == LeadStage.LOST) {
            if (currentUser.getRole() == UserRole.SALES_REP) {
                throw new AccessDeniedException("Only MANAGER or ADMIN can reopen WON or LOST leads");
            }
            return;
        }

        // Determine if this is a forward or backward move
        int currentOrdinal = stageOrdinal(current);
        int targetOrdinal = stageOrdinal(target);

        if (targetOrdinal < currentOrdinal) {
            // Backward move requires MANAGER or ADMIN
            if (currentUser.getRole() == UserRole.SALES_REP) {
                throw new AccessDeniedException("Only MANAGER or ADMIN can move leads backward");
            }
            return;
        }

        // Forward move: must be exactly one step forward (no skipping)
        LeadStage expectedNext = NEXT_STAGE.get(current);
        if (expectedNext == null || expectedNext != target) {
            String requiredPrev = REQUIRED_PREVIOUS.get(target);
            if (requiredPrev != null) {
                throw new BadRequestException(
                        "Cannot skip stages. Must go through " + requiredPrev + " first.");
            }
            throw new BadRequestException("Invalid stage transition from " + current.name() + " to " + target.name());
        }

        // WON requires value > 0 — checked in changeStage with access to the lead entity
    }

    private int stageOrdinal(LeadStage stage) {
        return switch (stage) {
            case NEW -> 0;
            case CONTACTED -> 1;
            case QUALIFIED -> 2;
            case PROPOSAL -> 3;
            case WON -> 4;
            case LOST -> 5;
        };
    }

    private LeadStage parseStageFilter(String stageFilter) {
        if (stageFilter == null || stageFilter.isBlank()) {
            return null;
        }
        try {
            return LeadStage.valueOf(stageFilter.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid stage: " + stageFilter);
        }
    }

    private Lead findLeadOrThrow(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
    }

    private void checkAccess(Lead lead, User currentUser) {
        if (!canAccessLead(lead, currentUser)) {
            throw new AccessDeniedException("You do not have access to this lead");
        }
    }

    boolean canAccessLead(Lead lead, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            return true;
        }
        return lead.getOwner() != null
                && lead.getOwner().getId().equals(currentUser.getId());
    }

    private List<UUID> getVisibleUserIds(User currentUser) {
        List<UUID> visibleUserIds = new ArrayList<>();
        visibleUserIds.add(currentUser.getId());
        userRepository.findByRole(UserRole.SALES_REP)
                .forEach(u -> visibleUserIds.add(u.getId()));
        return visibleUserIds;
    }
}
