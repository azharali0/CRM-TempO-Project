package com.crm.scheduler;

import com.crm.model.entity.Task;
import com.crm.model.enums.TaskStatus;
import com.crm.repository.TaskRepository;
import com.crm.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EmailReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailReminderScheduler.class);

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    public EmailReminderScheduler(TaskRepository taskRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyTaskReminders() {
        try {
            LocalDateTime dayStart = LocalDate.now().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<Task> tasksDueToday = taskRepository.findByDueDateBeforeAndStatusIn(
                    dayEnd, List.of(TaskStatus.PENDING));

            // Filter to only tasks due today (not past)
            List<Task> todayTasks = tasksDueToday.stream()
                    .filter(t -> t.getDueDate() != null && !t.getDueDate().isBefore(dayStart))
                    .toList();

            Map<UUID, List<Task>> grouped = todayTasks.stream()
                    .filter(t -> t.getAssignedTo() != null)
                    .collect(Collectors.groupingBy(t -> t.getAssignedTo().getId()));

            int emailCount = 0;
            for (Map.Entry<UUID, List<Task>> entry : grouped.entrySet()) {
                String email = entry.getValue().get(0).getAssignedTo().getEmail();
                int taskCount = entry.getValue().size();

                // Never include PII — generic message only
                emailService.sendEmail(
                        email,
                        "CRM: Tasks Due Today",
                        "You have " + taskCount + " task(s) due today. Please log in to view details."
                );
                emailCount++;
            }

            log.info("Sent daily task reminders to {} users for {} tasks", emailCount, todayTasks.size());
        } catch (Exception e) {
            log.error("Error in daily task reminder scheduler: {}", e.getMessage());
        }
    }
}
