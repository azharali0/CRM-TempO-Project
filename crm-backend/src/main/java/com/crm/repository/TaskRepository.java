package com.crm.repository;

import com.crm.model.entity.Task;
import com.crm.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByAssignedToId(UUID userId, Pageable pageable);

    Page<Task> findByAssignedToIdIn(List<UUID> userIds, Pageable pageable);

    List<Task> findByDueDateBeforeAndStatusIn(LocalDateTime date, List<TaskStatus> statuses);

    long countByAssignedToIdAndStatus(UUID userId, TaskStatus status);

    Page<Task> findByAssignedToIdAndStatus(UUID userId, TaskStatus status, Pageable pageable);

    Page<Task> findByAssignedToIdInAndStatus(List<UUID> userIds, TaskStatus status, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
