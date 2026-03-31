package com.crm.service;

import com.crm.dto.request.TaskCreateRequest;
import com.crm.dto.request.TaskUpdateRequest;
import com.crm.dto.response.TaskDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.BadRequestException;
import com.crm.exception.ConflictException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Task;
import com.crm.model.entity.User;
import com.crm.model.enums.TaskStatus;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.TaskRepository;
import com.crm.repository.UserRepository;
import com.crm.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       CustomerRepository customerRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskDTO createTask(TaskCreateRequest request, User currentUser) {
        User assignedTo = userRepository.findById(request.getAssignedTo())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found with id: " + request.getAssignedTo()));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        }

        Task task = Task.builder()
                .title(InputSanitizer.sanitize(request.getTitle()))
                .description(InputSanitizer.sanitizeOrNull(request.getDescription()))
                .dueDate(request.getDueDate())
                .priority(request.getPriority() != null ? request.getPriority() : com.crm.model.enums.TaskPriority.MEDIUM)
                .customer(customer)
                .assignedTo(assignedTo)
                .createdBy(currentUser)
                .build();

        Task saved = taskRepository.save(task);
        return TaskDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<TaskDTO> getMyTasks(Pageable pageable, String statusFilter, User currentUser) {
        TaskStatus status = parseStatusFilter(statusFilter);

        if (status != null) {
            return taskRepository.findByAssignedToIdAndStatus(currentUser.getId(), status, pageable)
                    .map(TaskDTO::fromEntity);
        }
        return taskRepository.findByAssignedToId(currentUser.getId(), pageable)
                .map(TaskDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TaskDTO> getAllTasks(Pageable pageable, String statusFilter, User currentUser) {
        if (currentUser.getRole() == UserRole.SALES_REP) {
            throw new AccessDeniedException("Only MANAGER or ADMIN can view all tasks");
        }

        TaskStatus status = parseStatusFilter(statusFilter);

        switch (currentUser.getRole()) {
            case ADMIN:
                return status != null
                        ? taskRepository.findByStatus(status, pageable).map(TaskDTO::fromEntity)
                        : taskRepository.findAll(pageable).map(TaskDTO::fromEntity);
            case MANAGER:
            default:
                List<UUID> visibleUserIds = getVisibleUserIds(currentUser);
                return status != null
                        ? taskRepository.findByAssignedToIdInAndStatus(visibleUserIds, status, pageable).map(TaskDTO::fromEntity)
                        : taskRepository.findByAssignedToIdIn(visibleUserIds, pageable).map(TaskDTO::fromEntity);
        }
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(UUID id, User currentUser) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, currentUser);
        return TaskDTO.fromEntity(task);
    }

    @Transactional
    public TaskDTO updateTask(UUID id, TaskUpdateRequest request, User currentUser) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, currentUser);

        if (request.getVersion() == null || !request.getVersion().equals(task.getVersion())) {
            throw new ConflictException("The resource was modified by another user. Please retry.");
        }

        if (request.getTitle() != null) {
            task.setTitle(InputSanitizer.sanitize(request.getTitle()));
        }
        if (request.getDescription() != null) {
            task.setDescription(InputSanitizer.sanitizeOrNull(request.getDescription()));
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssignedTo() != null) {
            User assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found with id: " + request.getAssignedTo()));
            task.setAssignedTo(assignedTo);
        }

        Task saved = taskRepository.save(task);
        return TaskDTO.fromEntity(saved);
    }

    @Transactional
    public TaskDTO completeTask(UUID id, User currentUser) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, currentUser);

        if (task.getStatus() == TaskStatus.DONE) {
            throw new BadRequestException("Task is already completed");
        }

        task.setStatus(TaskStatus.DONE);
        Task saved = taskRepository.save(task);
        return TaskDTO.fromEntity(saved);
    }

    @Transactional
    public int markOverdueTasks() {
        List<Task> overdueTasks = taskRepository.findByDueDateBeforeAndStatusIn(
                LocalDateTime.now(),
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)
        );

        for (Task task : overdueTasks) {
            task.setStatus(TaskStatus.OVERDUE);
        }
        taskRepository.saveAll(overdueTasks);

        return overdueTasks.size();
    }

    private TaskStatus parseStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return null;
        }
        try {
            return TaskStatus.valueOf(statusFilter.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + statusFilter);
        }
    }

    private Task findTaskOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private void checkAccess(Task task, User currentUser) {
        if (!canAccessTask(task, currentUser)) {
            throw new AccessDeniedException("You do not have access to this task");
        }
    }

    boolean canAccessTask(Task task, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            return true;
        }
        // SALES_REP can access tasks assigned to them or created by them
        return (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUser.getId()))
                || (task.getCreatedBy() != null && task.getCreatedBy().getId().equals(currentUser.getId()));
    }

    private List<UUID> getVisibleUserIds(User currentUser) {
        List<UUID> visibleUserIds = new ArrayList<>();
        visibleUserIds.add(currentUser.getId());
        userRepository.findByRole(UserRole.SALES_REP)
                .forEach(u -> visibleUserIds.add(u.getId()));
        return visibleUserIds;
    }
}
