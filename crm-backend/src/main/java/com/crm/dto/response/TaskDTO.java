package com.crm.dto.response;

import com.crm.model.entity.Task;
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
public class TaskDTO {

    private UUID id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private String priority;
    private String status;
    private String customerName;
    private UUID customerId;
    private String assignedToName;
    private UUID assignedToId;
    private String createdByName;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskDTO fromEntity(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .customerName(task.getCustomer() != null ? task.getCustomer().getName() : null)
                .customerId(task.getCustomer() != null ? task.getCustomer().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? task.getAssignedTo().getName() : null)
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .createdByName(task.getCreatedBy() != null ? task.getCreatedBy().getName() : null)
                .version(task.getVersion())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
