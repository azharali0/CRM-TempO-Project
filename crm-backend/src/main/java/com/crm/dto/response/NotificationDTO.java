package com.crm.dto.response;

import com.crm.model.entity.Notification;
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
public class NotificationDTO {

    private UUID id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationDTO fromEntity(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .read(Boolean.TRUE.equals(n.getRead()))
                .createdAt(n.getCreatedAt())
                .build();
    }
}
