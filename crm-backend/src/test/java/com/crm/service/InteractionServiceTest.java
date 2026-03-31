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
import com.crm.model.enums.InteractionType;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.InteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private InteractionService interactionService;

    private User salesRepUser;
    private User adminUser;
    private Customer testCustomer;
    private Interaction testInteraction;

    @BeforeEach
    void setUp() {
        salesRepUser = User.builder()
                .id(UUID.randomUUID()).name("Sales Rep").role(UserRole.SALES_REP).build();
        adminUser = User.builder()
                .id(UUID.randomUUID()).name("Admin").role(UserRole.ADMIN).build();

        testCustomer = Customer.builder()
                .id(UUID.randomUUID()).name("Test Customer").assignedTo(salesRepUser).build();

        testInteraction = Interaction.builder()
                .id(UUID.randomUUID())
                .customer(testCustomer)
                .type(InteractionType.CALL)
                .subject("Test Call")
                .notes("Test notes")
                .loggedBy(salesRepUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createInteraction_success() {
        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setCustomerId(testCustomer.getId());
        request.setType(InteractionType.CALL);
        request.setSubject("New Call");
        request.setNotes("Call notes");

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(interactionRepository.countByLoggedByIdAndCreatedAtAfter(eq(salesRepUser.getId()), any()))
                .thenReturn(0L);
        when(interactionRepository.save(any(Interaction.class))).thenReturn(testInteraction);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        InteractionDTO result = interactionService.createInteraction(request, salesRepUser);

        assertNotNull(result);
        verify(customerRepository).save(testCustomer); // lastContactedAt updated
    }

    @Test
    void createInteraction_customerNotFound_throws404() {
        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setType(InteractionType.CALL);

        when(customerRepository.findById(request.getCustomerId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> interactionService.createInteraction(request, salesRepUser));
    }

    @Test
    void createInteraction_rateLimitExceeded() {
        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setCustomerId(testCustomer.getId());
        request.setType(InteractionType.CALL);

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(interactionRepository.countByLoggedByIdAndCreatedAtAfter(eq(salesRepUser.getId()), any()))
                .thenReturn(100L);

        assertThrows(BadRequestException.class,
                () -> interactionService.createInteraction(request, salesRepUser));
    }

    @Test
    void createInteraction_xssInNotes_sanitized() {
        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setCustomerId(testCustomer.getId());
        request.setType(InteractionType.NOTE);
        request.setNotes("<script>alert('xss')</script>Important note");

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(interactionRepository.countByLoggedByIdAndCreatedAtAfter(eq(salesRepUser.getId()), any()))
                .thenReturn(0L);
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> {
            Interaction saved = invocation.getArgument(0);
            assertFalse(saved.getNotes().contains("<script>"));
            return testInteraction;
        });
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        interactionService.createInteraction(request, salesRepUser);
        verify(interactionRepository).save(any(Interaction.class));
    }

    @Test
    void updateInteraction_within24Hours_success() {
        testInteraction.setCreatedAt(LocalDateTime.now().minusHours(1));

        InteractionUpdateRequest request = new InteractionUpdateRequest();
        request.setSubject("Updated Subject");

        when(interactionRepository.findById(testInteraction.getId())).thenReturn(Optional.of(testInteraction));
        when(interactionRepository.save(any(Interaction.class))).thenReturn(testInteraction);

        InteractionDTO result = interactionService.updateInteraction(
                testInteraction.getId(), request, salesRepUser);

        assertNotNull(result);
    }

    @Test
    void updateInteraction_after24Hours_forbidden() {
        testInteraction.setCreatedAt(LocalDateTime.now().minusHours(25));

        InteractionUpdateRequest request = new InteractionUpdateRequest();
        request.setSubject("Updated Subject");

        when(interactionRepository.findById(testInteraction.getId())).thenReturn(Optional.of(testInteraction));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> interactionService.updateInteraction(
                        testInteraction.getId(), request, salesRepUser));
        assertTrue(ex.getMessage().contains("24 hour"));
    }

    @Test
    void updateInteraction_notAuthor_forbidden() {
        testInteraction.setCreatedAt(LocalDateTime.now().minusHours(1));
        User otherUser = User.builder()
                .id(UUID.randomUUID()).name("Other User").role(UserRole.SALES_REP).build();

        InteractionUpdateRequest request = new InteractionUpdateRequest();
        request.setSubject("Updated Subject");

        when(interactionRepository.findById(testInteraction.getId())).thenReturn(Optional.of(testInteraction));

        assertThrows(AccessDeniedException.class,
                () -> interactionService.updateInteraction(
                        testInteraction.getId(), request, otherUser));
    }

    @Test
    void updateInteraction_adminCanEditOthers() {
        testInteraction.setCreatedAt(LocalDateTime.now().minusHours(1));

        InteractionUpdateRequest request = new InteractionUpdateRequest();
        request.setSubject("Admin Update");

        when(interactionRepository.findById(testInteraction.getId())).thenReturn(Optional.of(testInteraction));
        when(interactionRepository.save(any(Interaction.class))).thenReturn(testInteraction);

        InteractionDTO result = interactionService.updateInteraction(
                testInteraction.getId(), request, adminUser);

        assertNotNull(result);
    }
}
