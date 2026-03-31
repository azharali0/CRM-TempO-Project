package com.crm.repository;

import com.crm.model.entity.Task;
import com.crm.model.enums.TaskStatus;
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
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByAssignedToId(UUID userId, Pageable pageable);

    Page<Task> findByAssignedToIdIn(List<UUID> userIds, Pageable pageable);

    List<Task> findByDueDateBeforeAndStatusIn(LocalDateTime date, List<TaskStatus> statuses);

    long countByAssignedToIdAndStatus(UUID userId, TaskStatus status);

    Page<Task> findByAssignedToIdAndStatus(UUID userId, TaskStatus status, Pageable pageable);

    Page<Task> findByAssignedToIdInAndStatus(List<UUID> userIds, TaskStatus status, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    // --- Report queries ---

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate >= :dayStart AND t.dueDate < :dayEnd " +
           "AND t.status IN :statuses")
    long countDueToday(@Param("dayStart") LocalDateTime dayStart,
                       @Param("dayEnd") LocalDateTime dayEnd,
                       @Param("statuses") List<TaskStatus> statuses);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate >= :dayStart AND t.dueDate < :dayEnd " +
           "AND t.status IN :statuses AND t.assignedTo.id = :userId")
    long countDueTodayByUser(@Param("dayStart") LocalDateTime dayStart,
                             @Param("dayEnd") LocalDateTime dayEnd,
                             @Param("statuses") List<TaskStatus> statuses,
                             @Param("userId") UUID userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate >= :dayStart AND t.dueDate < :dayEnd " +
           "AND t.status IN :statuses AND t.assignedTo.id IN :userIds")
    long countDueTodayByUserIn(@Param("dayStart") LocalDateTime dayStart,
                               @Param("dayEnd") LocalDateTime dayEnd,
                               @Param("statuses") List<TaskStatus> statuses,
                               @Param("userIds") List<UUID> userIds);

    long countByStatus(TaskStatus status);

    long countByAssignedToIdInAndStatus(List<UUID> userIds, TaskStatus status);
}
