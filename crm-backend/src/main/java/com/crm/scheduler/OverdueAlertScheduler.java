package com.crm.scheduler;

import com.crm.model.entity.Task;
import com.crm.model.enums.TaskStatus;
import com.crm.repository.TaskRepository;
import com.crm.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OverdueAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueAlertScheduler.class);

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    public OverdueAlertScheduler(TaskRepository taskRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendOverdueAlerts() {
        try {
            List<Task> overdueTasks = taskRepository.findByDueDateBeforeAndStatusIn(
                    java.time.LocalDateTime.now(), List.of(TaskStatus.OVERDUE));

            Map<UUID, List<Task>> grouped = overdueTasks.stream()
                    .filter(t -> t.getAssignedTo() != null)
                    .collect(Collectors.groupingBy(t -> t.getAssignedTo().getId()));

            int emailCount = 0;
            for (Map.Entry<UUID, List<Task>> entry : grouped.entrySet()) {
                String email = entry.getValue().get(0).getAssignedTo().getEmail();
                int taskCount = entry.getValue().size();

                // Never include PII — generic message only
                emailService.sendEmail(
                        email,
                        "CRM: Overdue Tasks Alert",
                        "You have " + taskCount + " overdue task(s). Please take action."
                );
                emailCount++;
            }

            log.info("Sent overdue alerts to {} users for {} overdue tasks", emailCount, overdueTasks.size());
        } catch (Exception e) {
            log.error("Error in overdue alert scheduler: {}", e.getMessage());
        }
    }
}
