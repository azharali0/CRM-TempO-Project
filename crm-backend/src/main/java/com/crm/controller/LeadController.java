package com.crm.controller;

import com.crm.dto.request.LeadCreateRequest;
import com.crm.dto.request.LeadUpdateRequest;
import com.crm.dto.request.StageChangeRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.LeadDTO;
import com.crm.model.entity.User;
import com.crm.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LeadDTO>>> getLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String stage,
            Authentication authentication) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = buildPageable(page, clampedSize, sort);
        User currentUser = (User) authentication.getPrincipal();

        Page<LeadDTO> leads = leadService.getLeads(pageable, stage, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Leads retrieved successfully", leads));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeadDTO>> createLead(
            @Valid @RequestBody LeadCreateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        LeadDTO lead = leadService.createLead(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead created successfully", lead));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadDTO>> getLeadById(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        LeadDTO lead = leadService.getLeadById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lead retrieved successfully", lead));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadDTO>> updateLead(
            @PathVariable UUID id,
            @Valid @RequestBody LeadUpdateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        LeadDTO lead = leadService.updateLead(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lead updated successfully", lead));
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<ApiResponse<LeadDTO>> changeStage(
            @PathVariable UUID id,
            @Valid @RequestBody StageChangeRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        LeadDTO lead = leadService.changeStage(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lead stage updated successfully", lead));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String property = parts[0];
            Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, property));
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
