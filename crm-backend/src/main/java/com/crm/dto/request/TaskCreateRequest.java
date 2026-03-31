package com.crm.dto.request;

import com.crm.model.enums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in the future or present")
    private LocalDateTime dueDate;

    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    private UUID customerId;

    @NotNull(message = "Assigned user is required")
    private UUID assignedTo;
}
