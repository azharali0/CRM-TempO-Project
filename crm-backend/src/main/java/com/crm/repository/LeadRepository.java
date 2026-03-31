package com.crm.repository;

import com.crm.model.entity.Lead;
import com.crm.model.enums.LeadStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Lead> findByOwnerIdIn(List<UUID> ownerIds, Pageable pageable);

    List<Lead> findByStage(LeadStage stage);

    long countByStage(LeadStage stage);

    Page<Lead> findByOwnerIdAndStage(UUID ownerId, LeadStage stage, Pageable pageable);

    Page<Lead> findByOwnerIdInAndStage(List<UUID> ownerIds, LeadStage stage, Pageable pageable);

    Page<Lead> findByStage(LeadStage stage, Pageable pageable);
}
