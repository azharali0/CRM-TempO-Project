package com.crm.repository;

import com.crm.model.entity.Interaction;
import com.crm.model.enums.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {

    Page<Interaction> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<Interaction> findByLoggedByIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Interaction> findTop50ByLoggedByIdOrCustomerAssignedToIdOrderByCreatedAtDesc(UUID loggedById, UUID assignedToId);

    long countByLoggedByIdAndCreatedAtAfter(UUID userId, LocalDateTime after);

    // --- Report queries ---

    long countByCreatedAtAfter(LocalDateTime after);

    @Query("SELECT i.type, COUNT(i) FROM Interaction i WHERE i.createdAt >= :since GROUP BY i.type")
    List<Object[]> countByTypeAfter(@Param("since") LocalDateTime since);

    @Query("SELECT i.type, COUNT(i) FROM Interaction i WHERE i.loggedBy.id = :userId AND i.createdAt >= :since GROUP BY i.type")
    List<Object[]> countByTypeAndUserAfter(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT i.type, COUNT(i) FROM Interaction i WHERE i.loggedBy.id IN :userIds AND i.createdAt >= :since GROUP BY i.type")
    List<Object[]> countByTypeAndUserInAfter(@Param("userIds") List<UUID> userIds, @Param("since") LocalDateTime since);

    long countByLoggedByIdInAndCreatedAtAfter(List<UUID> userIds, LocalDateTime after);
}
