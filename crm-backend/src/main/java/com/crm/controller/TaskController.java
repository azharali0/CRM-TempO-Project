package com.crm.controller;

import com.crm.dto.request.TaskCreateRequest;
import com.crm.dto.request.TaskUpdateRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.TaskDTO;
import com.crm.model.entity.User;
import com.crm.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TaskDTO>>> getMyTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = buildPageable(page, clampedSize, sort);
        User currentUser = (User) authentication.getPrincipal();

        Page<TaskDTO> tasks = taskService.getMyTasks(pageable, status, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TaskDTO>>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = buildPageable(page, clampedSize, sort);
        User currentUser = (User) authentication.getPrincipal();

        Page<TaskDTO> tasks = taskService.getAllTasks(pageable, status, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        TaskDTO task = taskService.createTask(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", task));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTaskById(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        TaskDTO task = taskService.getTaskById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        TaskDTO task = taskService.updateTask(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", task));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskDTO>> completeTask(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        TaskDTO task = taskService.completeTask(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Task completed successfully", task));
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
