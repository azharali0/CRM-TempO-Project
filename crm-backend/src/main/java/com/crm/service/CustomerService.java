package com.crm.service;

import com.crm.dto.request.CustomerCreateRequest;
import com.crm.dto.request.CustomerUpdateRequest;
import com.crm.dto.response.CustomerDetailDTO;
import com.crm.dto.response.CustomerListDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.BadRequestException;
import com.crm.exception.ConflictException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.User;
import com.crm.model.enums.CustomerStatus;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.UserRepository;
import com.crm.repository.specification.CustomerSpecification;
import com.crm.util.InputSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerDetailDTO createCustomer(CustomerCreateRequest request, User currentUser) {
        String sanitizedEmail = InputSanitizer.sanitizeOrNull(request.getEmail());

        if (sanitizedEmail != null && customerRepository.existsByEmail(sanitizedEmail)) {
            throw new ConflictException("A customer with this email already exists");
        }

        User assignedTo = null;
        if (request.getAssignedTo() != null) {
            assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));
        }

        Customer customer = Customer.builder()
                .name(InputSanitizer.sanitize(request.getName()))
                .email(sanitizedEmail)
                .phone(InputSanitizer.sanitizeOrNull(request.getPhone()))
                .company(InputSanitizer.sanitizeOrNull(request.getCompany()))
                .address(InputSanitizer.sanitizeOrNull(request.getAddress()))
                .city(InputSanitizer.sanitizeOrNull(request.getCity()))
                .assignedTo(assignedTo)
                .createdBy(currentUser)
                .build();

        Customer saved = customerRepository.save(customer);
        return CustomerDetailDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerListDTO> getCustomers(Pageable pageable, String name, String company,
                                               String status, String city, UUID assignedToFilter,
                                               LocalDateTime from, LocalDateTime to,
                                               User currentUser) {
        Specification<Customer> spec = buildRoleBasedSpec(currentUser);

        if (name != null && !name.isBlank()) {
            spec = spec.and(CustomerSpecification.hasName(name));
        }
        if (company != null && !company.isBlank()) {
            spec = spec.and(CustomerSpecification.hasCompany(company));
        }
        if (status != null && !status.isBlank()) {
            try {
                CustomerStatus cs = CustomerStatus.valueOf(status.toUpperCase());
                spec = spec.and(CustomerSpecification.hasStatus(cs));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        }
        if (city != null && !city.isBlank()) {
            spec = spec.and(CustomerSpecification.hasCity(city));
        }
        if (assignedToFilter != null) {
            spec = spec.and(CustomerSpecification.assignedTo(assignedToFilter));
        }
        if (from != null && to != null) {
            spec = spec.and(CustomerSpecification.createdBetween(from, to));
        }

        return customerRepository.findAll(spec, pageable).map(CustomerListDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public CustomerDetailDTO getCustomerById(UUID id, User currentUser) {
        Customer customer = findCustomerOrThrow(id);
        checkAccess(customer, currentUser);
        return CustomerDetailDTO.fromEntity(customer);
    }

    @Transactional
    public CustomerDetailDTO updateCustomer(UUID id, CustomerUpdateRequest request, User currentUser) {
        Customer customer = findCustomerOrThrow(id);
        checkAccess(customer, currentUser);

        if (request.getVersion() == null || !request.getVersion().equals(customer.getVersion())) {
            throw new ConflictException("The resource was modified by another user. Please retry.");
        }

        if (request.getName() != null) {
            customer.setName(InputSanitizer.sanitize(request.getName()));
        }
        if (request.getEmail() != null) {
            String sanitizedEmail = InputSanitizer.sanitizeOrNull(request.getEmail());
            if (sanitizedEmail != null && customerRepository.existsByEmailAndIdNot(sanitizedEmail, id)) {
                throw new ConflictException("A customer with this email already exists");
            }
            customer.setEmail(sanitizedEmail);
        }
        if (request.getPhone() != null) {
            customer.setPhone(InputSanitizer.sanitizeOrNull(request.getPhone()));
        }
        if (request.getCompany() != null) {
            customer.setCompany(InputSanitizer.sanitizeOrNull(request.getCompany()));
        }
        if (request.getAddress() != null) {
            customer.setAddress(InputSanitizer.sanitizeOrNull(request.getAddress()));
        }
        if (request.getCity() != null) {
            customer.setCity(InputSanitizer.sanitizeOrNull(request.getCity()));
        }
        if (request.getAssignedTo() != null) {
            User assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));
            customer.setAssignedTo(assignedTo);
        }

        Customer saved = customerRepository.save(customer);
        return CustomerDetailDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntityWithAccessCheck(UUID id, User currentUser) {
        Customer customer = findCustomerOrThrow(id);
        checkAccess(customer, currentUser);
        return customer;
    }

    @Transactional
    public void deleteCustomer(UUID id, User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can delete customers");
        }

        Customer customer = findCustomerOrThrow(id);
        customer.setStatus(CustomerStatus.ARCHIVED);
        customerRepository.save(customer);
    }

    private Customer findCustomerOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private void checkAccess(Customer customer, User currentUser) {
        if (!canAccessCustomer(customer, currentUser)) {
            throw new AccessDeniedException("You do not have access to this customer");
        }
    }

    boolean canAccessCustomer(Customer customer, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            return true;
        }
        // SALES_REP can only access customers assigned to them
        return customer.getAssignedTo() != null
                && customer.getAssignedTo().getId().equals(currentUser.getId());
    }

    private Specification<Customer> buildRoleBasedSpec(User currentUser) {
        Specification<Customer> spec = Specification.where(null);

        switch (currentUser.getRole()) {
            case ADMIN:
                // No additional filtering
                break;
            case MANAGER:
                // Manager sees customers assigned to themselves or any SALES_REP
                List<UUID> visibleUserIds = new ArrayList<>();
                visibleUserIds.add(currentUser.getId());
                userRepository.findByRole(UserRole.SALES_REP)
                        .forEach(u -> visibleUserIds.add(u.getId()));
                spec = spec.and(CustomerSpecification.assignedToIn(visibleUserIds));
                break;
            case SALES_REP:
                spec = spec.and(CustomerSpecification.assignedTo(currentUser.getId()));
                break;
        }

        return spec;
    }
}
