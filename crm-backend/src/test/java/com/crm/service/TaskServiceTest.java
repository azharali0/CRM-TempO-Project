package com.crm.service;

import com.crm.dto.request.TaskCreateRequest;
import com.crm.dto.response.TaskDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.BadRequestException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Task;
import com.crm.model.entity.User;
import com.crm.model.enums.TaskPriority;
import com.crm.model.enums.TaskStatus;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.TaskRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User salesRepUser;
    private User adminUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        salesRepUser = User.builder()
                .id(UUID.randomUUID()).name("Sales Rep").role(UserRole.SALES_REP).build();
        adminUser = User.builder()
                .id(UUID.randomUUID()).name("Admin").role(UserRole.ADMIN).build();

        testTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .assignedTo(salesRepUser)
                .createdBy(adminUser)
                .version(0)
                .build();
    }

    @Test
    void createTask_success() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("New Task");
        request.setAssignedTo(salesRepUser.getId());
        request.setDueDate(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(salesRepUser.getId())).thenReturn(Optional.of(salesRepUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskDTO result = taskService.createTask(request, adminUser);

        assertNotNull(result);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void completeTask_success() {
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskDTO result = taskService.completeTask(testTask.getId(), salesRepUser);

        assertNotNull(result);
        assertEquals(TaskStatus.DONE, testTask.getStatus());
    }

    @Test
    void completeTask_alreadyDone_throwsBadRequest() {
        testTask.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));

        assertThrows(BadRequestException.class,
                () -> taskService.completeTask(testTask.getId(), salesRepUser));
    }

    @Test
    void markOverdueTasks_marksCorrectly() {
        Task overdueTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Overdue Task")
                .status(TaskStatus.PENDING)
                .dueDate(LocalDateTime.now().minusDays(1))
                .build();

        when(taskRepository.findByDueDateBeforeAndStatusIn(any(), any()))
                .thenReturn(List.of(overdueTask));
        when(taskRepository.saveAll(any())).thenReturn(List.of(overdueTask));

        int count = taskService.markOverdueTasks();

        assertEquals(1, count);
        assertEquals(TaskStatus.OVERDUE, overdueTask.getStatus());
    }

    @Test
    void getTaskById_salesRepOwn_success() {
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));

        TaskDTO result = taskService.getTaskById(testTask.getId(), salesRepUser);

        assertNotNull(result);
    }

    @Test
    void getTaskById_salesRepOther_forbidden() {
        User otherRep = User.builder()
                .id(UUID.randomUUID()).name("Other Rep").role(UserRole.SALES_REP).build();
        Task otherTask = Task.builder()
                .id(UUID.randomUUID())
                .assignedTo(otherRep)
                .createdBy(otherRep)
                .build();

        when(taskRepository.findById(otherTask.getId())).thenReturn(Optional.of(otherTask));

        assertThrows(AccessDeniedException.class,
                () -> taskService.getTaskById(otherTask.getId(), salesRepUser));
    }

    @Test
    void getAllTasks_salesRep_forbidden() {
        assertThrows(AccessDeniedException.class,
                () -> taskService.getAllTasks(null, null, salesRepUser));
    }
}
