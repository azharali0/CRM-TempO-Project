package com.crm.service;

import com.crm.dto.request.LeadCreateRequest;
import com.crm.dto.request.StageChangeRequest;
import com.crm.dto.response.LeadDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.BadRequestException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Lead;
import com.crm.model.entity.User;
import com.crm.model.enums.LeadStage;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeadService leadService;

    private User adminUser;
    private User salesRepUser;
    private User managerUser;
    private Customer testCustomer;
    private Lead testLead;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID()).name("Admin").role(UserRole.ADMIN).build();
        salesRepUser = User.builder()
                .id(UUID.randomUUID()).name("Sales Rep").role(UserRole.SALES_REP).build();
        managerUser = User.builder()
                .id(UUID.randomUUID()).name("Manager").role(UserRole.MANAGER).build();

        testCustomer = Customer.builder()
                .id(UUID.randomUUID()).name("Test Customer").build();

        testLead = Lead.builder()
                .id(UUID.randomUUID())
                .customer(testCustomer)
                .title("Test Lead")
                .value(new BigDecimal("5000"))
                .stage(LeadStage.NEW)
                .owner(salesRepUser)
                .version(0)
                .build();
    }

    @Test
    void createLead_success() {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setCustomerId(testCustomer.getId());
        request.setTitle("New Lead");
        request.setValue(new BigDecimal("5000"));

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadDTO result = leadService.createLead(request, salesRepUser);

        assertNotNull(result);
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void changeStage_validForwardMove() {
        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.CONTACTED);

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadDTO result = leadService.changeStage(testLead.getId(), request, salesRepUser);

        assertNotNull(result);
        assertEquals(LeadStage.CONTACTED, testLead.getStage());
    }

    @Test
    void changeStage_skipStages_throwsBadRequest() {
        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.PROPOSAL); // Skip CONTACTED and QUALIFIED

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));

        assertThrows(BadRequestException.class,
                () -> leadService.changeStage(testLead.getId(), request, salesRepUser));
    }

    @Test
    void changeStage_wonRequiresPositiveValue() {
        testLead.setStage(LeadStage.PROPOSAL);
        testLead.setValue(BigDecimal.ZERO);

        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.WON);

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));

        assertThrows(BadRequestException.class,
                () -> leadService.changeStage(testLead.getId(), request, salesRepUser));
    }

    @Test
    void changeStage_lostRequiresReason() {
        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.LOST);
        // No lost reason

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));

        assertThrows(BadRequestException.class,
                () -> leadService.changeStage(testLead.getId(), request, salesRepUser));
    }

    @Test
    void changeStage_lostWithReason_success() {
        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.LOST);
        request.setLostReason("Customer not interested");

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadDTO result = leadService.changeStage(testLead.getId(), request, salesRepUser);

        assertNotNull(result);
        assertEquals(LeadStage.LOST, testLead.getStage());
    }

    @Test
    void changeStage_backwardMoveBySalesRep_forbidden() {
        testLead.setStage(LeadStage.QUALIFIED);

        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.CONTACTED);

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));

        assertThrows(AccessDeniedException.class,
                () -> leadService.changeStage(testLead.getId(), request, salesRepUser));
    }

    @Test
    void changeStage_backwardMoveByManager_allowed() {
        testLead.setStage(LeadStage.QUALIFIED);

        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.CONTACTED);

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(testLead);

        LeadDTO result = leadService.changeStage(testLead.getId(), request, managerUser);

        assertNotNull(result);
        assertEquals(LeadStage.CONTACTED, testLead.getStage());
    }

    @Test
    void changeStage_sameStage_throwsBadRequest() {
        StageChangeRequest request = new StageChangeRequest();
        request.setNewStage(LeadStage.NEW);

        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));

        assertThrows(BadRequestException.class,
                () -> leadService.changeStage(testLead.getId(), request, salesRepUser));
    }

    @Test
    void getLeadById_salesRepAccessOwn() {
        when(leadRepository.findById(testLead.getId())).thenReturn(Optional.of(testLead));

        LeadDTO result = leadService.getLeadById(testLead.getId(), salesRepUser);

        assertNotNull(result);
    }

    @Test
    void getLeadById_salesRepAccessOther_forbidden() {
        User otherRep = User.builder()
                .id(UUID.randomUUID()).name("Other Rep").role(UserRole.SALES_REP).build();
        Lead otherLead = Lead.builder()
                .id(UUID.randomUUID()).owner(otherRep).build();

        when(leadRepository.findById(otherLead.getId())).thenReturn(Optional.of(otherLead));

        assertThrows(AccessDeniedException.class,
                () -> leadService.getLeadById(otherLead.getId(), salesRepUser));
    }
}
