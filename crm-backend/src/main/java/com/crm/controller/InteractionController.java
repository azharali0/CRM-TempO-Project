package com.crm.controller;

import com.crm.dto.request.InteractionCreateRequest;
import com.crm.dto.request.InteractionUpdateRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.InteractionDTO;
import com.crm.model.entity.User;
import com.crm.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InteractionDTO>> createInteraction(
            @Valid @RequestBody InteractionCreateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        InteractionDTO interaction = interactionService.createInteraction(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interaction created successfully", interaction));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<Page<InteractionDTO>>> getCustomerInteractions(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        User currentUser = (User) authentication.getPrincipal();

        Page<InteractionDTO> interactions = interactionService.getCustomerInteractions(customerId, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Interactions retrieved successfully", interactions));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<InteractionDTO>>> getRecentInteractions(
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        List<InteractionDTO> interactions = interactionService.getRecentInteractions(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Recent interactions retrieved successfully", interactions));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InteractionDTO>> updateInteraction(
            @PathVariable UUID id,
            @Valid @RequestBody InteractionUpdateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        InteractionDTO interaction = interactionService.updateInteraction(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Interaction updated successfully", interaction));
    }
}
