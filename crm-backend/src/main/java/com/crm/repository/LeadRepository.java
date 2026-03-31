package com.crm.repository;

import com.crm.model.entity.Lead;
import com.crm.model.enums.LeadStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // --- Report queries ---

    long countByStageNotIn(List<LeadStage> stages);

    long countByOwnerIdAndStageNotIn(UUID ownerId, List<LeadStage> stages);

    long countByOwnerIdInAndStageNotIn(List<UUID> ownerIds, List<LeadStage> stages);

    @Query("SELECT COALESCE(SUM(l.value), 0) FROM Lead l WHERE l.stage = :stage")
    BigDecimal sumValueByStage(@Param("stage") LeadStage stage);

    @Query("SELECT COALESCE(SUM(l.value), 0) FROM Lead l WHERE l.stage = :stage AND l.owner.id = :ownerId")
    BigDecimal sumValueByStageAndOwnerId(@Param("stage") LeadStage stage, @Param("ownerId") UUID ownerId);

    @Query("SELECT COALESCE(SUM(l.value), 0) FROM Lead l WHERE l.stage = :stage AND l.owner.id IN :ownerIds")
    BigDecimal sumValueByStageAndOwnerIdIn(@Param("stage") LeadStage stage, @Param("ownerIds") List<UUID> ownerIds);

    long countByOwnerId(UUID ownerId);

    long countByOwnerIdIn(List<UUID> ownerIds);

    long countByOwnerIdAndStage(UUID ownerId, LeadStage stage);

    long countByOwnerIdInAndStage(List<UUID> ownerIds, LeadStage stage);

    List<Lead> findByStageAndOwnerId(LeadStage stage, UUID ownerId);

    List<Lead> findByStageAndOwnerIdIn(LeadStage stage, List<UUID> ownerIds);

    @Query("SELECT l FROM Lead l WHERE l.stage = :stage AND l.updatedAt >= :since")
    List<Lead> findByStageAndUpdatedAtAfter(@Param("stage") LeadStage stage, @Param("since") LocalDateTime since);

    @Query("SELECT l FROM Lead l WHERE l.stage = :stage AND l.owner.id = :ownerId AND l.updatedAt >= :since")
    List<Lead> findByStageAndOwnerIdAndUpdatedAtAfter(@Param("stage") LeadStage stage,
                                                       @Param("ownerId") UUID ownerId,
                                                       @Param("since") LocalDateTime since);

    @Query("SELECT l FROM Lead l WHERE l.stage = :stage AND l.owner.id IN :ownerIds AND l.updatedAt >= :since")
    List<Lead> findByStageAndOwnerIdInAndUpdatedAtAfter(@Param("stage") LeadStage stage,
                                                         @Param("ownerIds") List<UUID> ownerIds,
                                                         @Param("since") LocalDateTime since);

    @Query("SELECT l.owner.id, l.owner.name, COUNT(l), COALESCE(SUM(l.value), 0) " +
           "FROM Lead l WHERE l.stage = com.crm.model.enums.LeadStage.WON GROUP BY l.owner.id, l.owner.name")
    List<Object[]> findWonLeadsGroupedByOwner();

    @Query("SELECT l.owner.id, l.owner.name, COUNT(l), COALESCE(SUM(l.value), 0) " +
           "FROM Lead l WHERE l.stage = com.crm.model.enums.LeadStage.WON AND l.owner.id IN :ownerIds " +
           "GROUP BY l.owner.id, l.owner.name")
    List<Object[]> findWonLeadsGroupedByOwnerIn(@Param("ownerIds") List<UUID> ownerIds);
}
