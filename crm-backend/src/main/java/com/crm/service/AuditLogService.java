package com.crm.service;

import com.crm.dto.response.AuditLogDTO;
import com.crm.model.entity.AuditLog;
import com.crm.model.entity.User;
import com.crm.model.enums.UserRole;
import com.crm.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @Transactional
    public void logAction(String entityType, UUID entityId, String action,
                          String fieldName, String oldValue, String newValue,
                          User performedBy, String ipAddress) {
        // CRITICAL: Never log password field values
        if ("password".equalsIgnoreCase(fieldName)) {
            action = "PASSWORD_CHANGED";
            oldValue = null;
            newValue = null;
        }

        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(String entityType, UUID entityId,
                                           UUID performedById,
                                           LocalDateTime from, LocalDateTime to,
                                           User currentUser, Pageable pageable) {
        // MANAGER can view audit for their team. ADMIN sees all.
        if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.MANAGER) {
            throw new com.crm.exception.AccessDeniedException("Only MANAGER or ADMIN can view audit logs");
        }

        Specification<AuditLog> spec = Specification.where(null);

        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityType"), entityType));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityId"), entityId));
        }
        if (performedById != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("performedBy").get("id"), performedById));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("performedAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("performedAt"), to));
        }

        return auditLogRepository.findAll(spec, pageable).map(AuditLogDTO::fromEntity);
    }
}
