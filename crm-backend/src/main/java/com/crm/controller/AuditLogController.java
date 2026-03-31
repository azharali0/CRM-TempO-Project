package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.AuditLogDTO;
import com.crm.model.entity.User;
import com.crm.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID performedBy,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        Page<AuditLogDTO> auditLogs = auditLogService.getAuditLogs(
                entityType, entityId, performedBy, from, to, currentUser,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "performedAt")));

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", auditLogs));
    }
}
