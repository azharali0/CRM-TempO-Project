package com.crm.service;

import com.crm.dto.request.CustomerCreateRequest;
import com.crm.dto.request.CustomerUpdateRequest;
import com.crm.dto.response.CustomerDetailDTO;
import com.crm.dto.response.CustomerListDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.ConflictException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.User;
import com.crm.model.enums.CustomerStatus;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    private User adminUser;
    private User salesRepUser;
    private User managerUser;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID()).name("Admin").email("admin@test.com")
                .role(UserRole.ADMIN).enabled(true).failedLoginAttempts(0).build();

        salesRepUser = User.builder()
                .id(UUID.randomUUID()).name("Sales Rep").email("sales@test.com")
                .role(UserRole.SALES_REP).enabled(true).failedLoginAttempts(0).build();

        managerUser = User.builder()
                .id(UUID.randomUUID()).name("Manager").email("manager@test.com")
                .role(UserRole.MANAGER).enabled(true).failedLoginAttempts(0).build();

        testCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Test Customer")
                .email("customer@test.com")
                .phone("0300-1234567")
                .company("Test Corp")
                .status(CustomerStatus.ACTIVE)
                .assignedTo(salesRepUser)
                .createdBy(salesRepUser)
                .version(0)
                .build();
    }

    @Test
    void createCustomer_success() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setName("New Customer");
        request.setEmail("new@test.com");
        request.setPhone("0300-9876543");
        request.setCompany("New Corp");

        when(customerRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerDetailDTO result = customerService.createCustomer(request, salesRepUser);

        assertNotNull(result);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_duplicateEmail_throwsConflict() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setName("New Customer");
        request.setEmail("existing@test.com");

        when(customerRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> customerService.createCustomer(request, salesRepUser));
    }

    @Test
    void createCustomer_xssInName_sanitized() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setName("<script>alert('xss')</script>Customer");
        request.setEmail("xss@test.com");

        when(customerRepository.existsByEmail("xss@test.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            assertFalse(saved.getName().contains("<script>"));
            return testCustomer;
        });

        customerService.createCustomer(request, salesRepUser);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void getCustomerById_salesRepAccessOwn_success() {
        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));

        CustomerDetailDTO result = customerService.getCustomerById(testCustomer.getId(), salesRepUser);

        assertNotNull(result);
    }

    @Test
    void getCustomerById_salesRepAccessOther_forbidden() {
        User otherRep = User.builder()
                .id(UUID.randomUUID()).name("Other Rep").role(UserRole.SALES_REP).build();
        Customer otherCustomer = Customer.builder()
                .id(UUID.randomUUID()).name("Other").assignedTo(otherRep).build();

        when(customerRepository.findById(otherCustomer.getId())).thenReturn(Optional.of(otherCustomer));

        assertThrows(AccessDeniedException.class,
                () -> customerService.getCustomerById(otherCustomer.getId(), salesRepUser));
    }

    @Test
    void getCustomerById_admin_canAccessAny() {
        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));

        CustomerDetailDTO result = customerService.getCustomerById(testCustomer.getId(), adminUser);

        assertNotNull(result);
    }

    @Test
    void getCustomerById_notFound_throws404() {
        UUID nonExistentId = UUID.randomUUID();
        when(customerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> customerService.getCustomerById(nonExistentId, adminUser));
    }

    @Test
    void updateCustomer_optimisticLockConflict() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setName("Updated Name");
        request.setVersion(99); // Wrong version

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));

        assertThrows(ConflictException.class,
                () -> customerService.updateCustomer(testCustomer.getId(), request, salesRepUser));
    }

    @Test
    void updateCustomer_correctVersion_success() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setName("Updated Name");
        request.setVersion(0);

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerDetailDTO result = customerService.updateCustomer(testCustomer.getId(), request, salesRepUser);

        assertNotNull(result);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void deleteCustomer_adminOnly_success() {
        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        customerService.deleteCustomer(testCustomer.getId(), adminUser);

        assertEquals(CustomerStatus.ARCHIVED, testCustomer.getStatus());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void deleteCustomer_salesRep_forbidden() {
        assertThrows(AccessDeniedException.class,
                () -> customerService.deleteCustomer(testCustomer.getId(), salesRepUser));
    }

    @Test
    void deleteCustomer_isSoftDelete() {
        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        customerService.deleteCustomer(testCustomer.getId(), adminUser);

        // Verify it's a soft delete (status changed to ARCHIVED)
        assertEquals(CustomerStatus.ARCHIVED, testCustomer.getStatus());
        // Verify the record was not physically deleted
        verify(customerRepository, never()).delete(any(Customer.class));
        verify(customerRepository, never()).deleteById(any(UUID.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getCustomers_salesRepSeesOnlyOwn() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<CustomerListDTO> result = customerService.getCustomers(
                PageRequest.of(0, 20), null, null, null, null, null, null, null, salesRepUser);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getCustomers_phoneMaskedInListDTO() {
        CustomerListDTO dto = CustomerListDTO.fromEntity(testCustomer);

        assertNotNull(dto.getMaskedPhone());
        assertTrue(dto.getMaskedPhone().contains("***"));
        assertNotEquals("0300-1234567", dto.getMaskedPhone());
    }
}
