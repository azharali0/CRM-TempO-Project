package com.crm.service;

import com.crm.dto.request.InteractionCreateRequest;
import com.crm.dto.request.InteractionUpdateRequest;
import com.crm.dto.response.InteractionDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Interaction;
import com.crm.model.entity.User;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.InteractionRepository;
import com.crm.util.InputSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    private static final int MAX_INTERACTIONS_PER_HOUR = 100;

    private final InteractionRepository interactionRepository;
    private final CustomerRepository customerRepository;

    public InteractionService(InteractionRepository interactionRepository,
                              CustomerRepository customerRepository) {
        this.interactionRepository = interactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public InteractionDTO createInteraction(InteractionCreateRequest request, User currentUser) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        checkCustomerAccess(customer, currentUser);

        // Rate limit: max 100 interactions per user per hour
        long recentCount = interactionRepository.countByLoggedByIdAndCreatedAtAfter(
                currentUser.getId(), LocalDateTime.now().minusHours(1));
        if (recentCount >= MAX_INTERACTIONS_PER_HOUR) {
            throw new BadRequestException("Rate limit exceeded: maximum " + MAX_INTERACTIONS_PER_HOUR + " interactions per hour");
        }

        Interaction interaction = Interaction.builder()
                .customer(customer)
                .type(request.getType())
                .subject(InputSanitizer.sanitizeOrNull(request.getSubject()))
                .notes(InputSanitizer.sanitizeOrNull(request.getNotes()))
                .duration(request.getDuration())
                .loggedBy(currentUser)
                .build();

        Interaction saved = interactionRepository.save(interaction);

        // Update customer.lastContactedAt atomically in same transaction
        customer.setLastContactedAt(LocalDateTime.now());
        customerRepository.save(customer);

        return InteractionDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<InteractionDTO> getCustomerInteractions(UUID customerId, Pageable pageable, User currentUser) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        checkCustomerAccess(customer, currentUser);

        return interactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(InteractionDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<InteractionDTO> getRecentInteractions(User currentUser) {
        List<Interaction> interactions;

        switch (currentUser.getRole()) {
            case ADMIN:
                interactions = interactionRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 50,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                        .getContent();
                break;
            case MANAGER:
            case SALES_REP:
            default:
                interactions = interactionRepository
                        .findTop50ByLoggedByIdOrCustomerAssignedToIdOrderByCreatedAtDesc(
                                currentUser.getId(), currentUser.getId());
                break;
        }

        return interactions.stream()
                .map(InteractionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InteractionDTO updateInteraction(UUID id, InteractionUpdateRequest request, User currentUser) {
        Interaction interaction = interactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction not found with id: " + id));

        // 24-hour edit window
        if (interaction.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new AccessDeniedException("Cannot edit — 24 hour edit window has expired");
        }

        // Only the logger or an ADMIN can edit
        boolean isLogger = interaction.getLoggedBy() != null
                && interaction.getLoggedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isLogger && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to edit this interaction");
        }

        if (request.getSubject() != null) {
            interaction.setSubject(InputSanitizer.sanitizeOrNull(request.getSubject()));
        }
        if (request.getNotes() != null) {
            interaction.setNotes(InputSanitizer.sanitizeOrNull(request.getNotes()));
        }
        if (request.getDuration() != null) {
            interaction.setDuration(request.getDuration());
        }

        Interaction saved = interactionRepository.save(interaction);
        return InteractionDTO.fromEntity(saved);
    }

    private void checkCustomerAccess(Customer customer, User currentUser) {
        if (!canAccessCustomer(customer, currentUser)) {
            throw new AccessDeniedException("You do not have access to this customer");
        }
    }

    private boolean canAccessCustomer(Customer customer, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            return true;
        }
        return customer.getAssignedTo() != null
                && customer.getAssignedTo().getId().equals(currentUser.getId());
    }
}
