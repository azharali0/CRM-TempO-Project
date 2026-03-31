package com.crm.scheduler;

import com.crm.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueTaskScheduler.class);

    private final TaskService taskService;

    public OverdueTaskScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkOverdueTasks() {
        int count = taskService.markOverdueTasks();
        log.info("Marked {} tasks as overdue", count);
    }
}
