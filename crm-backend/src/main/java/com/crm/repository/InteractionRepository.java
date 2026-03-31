package com.crm.repository;

import com.crm.model.entity.Interaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
