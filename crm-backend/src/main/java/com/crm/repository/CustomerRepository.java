package com.crm.repository;

import com.crm.model.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Page<Customer> findByAssignedToId(UUID userId, Pageable pageable);

    Page<Customer> findByAssignedToIdIn(List<UUID> userIds, Pageable pageable);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByEmail(String email);

    long countByAssignedToId(UUID userId);

    long countByAssignedToIdIn(List<UUID> userIds);
}
