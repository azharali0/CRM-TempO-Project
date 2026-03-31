package com.crm.dto.request;

import com.crm.model.enums.TaskPriority;
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
public class TaskUpdateRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    private LocalDateTime dueDate;

    private TaskPriority priority;

    private UUID assignedTo;

    @NotNull(message = "Version is required for optimistic locking")
    private Integer version;
}
