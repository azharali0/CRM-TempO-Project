package com.crm.repository;

import com.crm.model.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByCustomerIdAndDeletedFalse(UUID customerId, Pageable pageable);

    List<Document> findByCustomerIdAndDeletedFalse(UUID customerId);

    Optional<Document> findByIdAndDeletedFalse(UUID id);
}
