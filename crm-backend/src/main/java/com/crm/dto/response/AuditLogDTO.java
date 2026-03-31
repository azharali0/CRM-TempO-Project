package com.crm.dto.response;

import com.crm.model.entity.AuditLog;
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
public class AuditLogDTO {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String performedByName;
    private LocalDateTime performedAt;
    private String ipAddress;

    public static AuditLogDTO fromEntity(AuditLog a) {
        return AuditLogDTO.builder()
                .id(a.getId())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .action(a.getAction())
                .fieldName(a.getFieldName())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .performedByName(a.getPerformedBy() != null ? a.getPerformedBy().getName() : null)
                .performedAt(a.getPerformedAt())
                .ipAddress(a.getIpAddress())
                .build();
    }
}
