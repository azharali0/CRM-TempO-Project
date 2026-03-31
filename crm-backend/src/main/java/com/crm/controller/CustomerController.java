package com.crm.controller;

import com.crm.dto.request.CustomerCreateRequest;
import com.crm.dto.request.CustomerUpdateRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.CustomerDetailDTO;
import com.crm.dto.response.CustomerListDTO;
import com.crm.model.entity.User;
import com.crm.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerListDTO>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Authentication authentication) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = buildPageable(page, clampedSize, sort);
        User currentUser = (User) authentication.getPrincipal();

        Page<CustomerListDTO> customers = customerService.getCustomers(
                pageable, name, company, status, city, assignedTo, from, to, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDetailDTO>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        CustomerDetailDTO customer = customerService.createCustomer(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", customer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDTO>> getCustomerById(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        CustomerDetailDTO customer = customerService.getCustomerById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDTO>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        CustomerDetailDTO customer = customerService.updateCustomer(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        customerService.deleteCustomer(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Customer archived successfully"));
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
