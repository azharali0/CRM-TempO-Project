package com.crm.service;

import com.crm.dto.response.NotificationDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Notification;
import com.crm.model.entity.Notification.NotificationType;
import com.crm.model.entity.User;
import com.crm.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Async
    @Transactional
    public void createNotification(User recipient, String title, String message, NotificationType type) {
        // Never include PII in notification messages
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getMyNotifications(User currentUser, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                currentUser.getId(), pageable).map(NotificationDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId());
    }

    @Transactional
    public NotificationDTO markAsRead(UUID notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You cannot modify this notification");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(saved);
    }

    // Convenience methods for auto-creating notifications
    public void notifyTaskAssigned(User assignee, String taskTitle) {
        createNotification(assignee, "New Task Assigned",
                "You have a new task. Please log in to view details.",
                NotificationType.TASK_ASSIGNED);
    }

    public void notifyLeadStageChanged(User owner, String leadTitle, String newStage) {
        createNotification(owner, "Lead Stage Updated",
                "A lead stage has been updated to " + newStage + ". Please log in to view details.",
                NotificationType.LEAD_STAGE_CHANGED);
    }

    public void notifyCustomerAssigned(User assignee) {
        createNotification(assignee, "Customer Assigned",
                "A new customer has been assigned to you. Please log in to view details.",
                NotificationType.CUSTOMER_ASSIGNED);
    }
}
