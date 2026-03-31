package com.crm.repository;

import com.crm.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
            String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByPerformedByIdOrderByPerformedAtDesc(UUID performedById, Pageable pageable);
}
