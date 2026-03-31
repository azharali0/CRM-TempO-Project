package com.crm.service;

import com.crm.dto.response.NotificationDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Notification;
import com.crm.model.entity.Notification.NotificationType;
import com.crm.model.entity.User;
import com.crm.model.enums.UserRole;
import com.crm.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID()).name("Test User").role(UserRole.SALES_REP).build();

        testNotification = Notification.builder()
                .id(UUID.randomUUID())
                .recipient(testUser)
                .title("New Task Assigned")
                .message("You have a new task. Please log in to view details.")
                .type(NotificationType.TASK_ASSIGNED)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getMyNotifications_returnsUserNotifications() {
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                testUser.getId(), PageRequest.of(0, 50))).thenReturn(page);

        Page<NotificationDTO> result = notificationService.getMyNotifications(
                testUser, PageRequest.of(0, 50));

        assertEquals(1, result.getContent().size());
        assertEquals("New Task Assigned", result.getContent().get(0).getTitle());
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(testUser.getId())).thenReturn(3L);

        long count = notificationService.getUnreadCount(testUser);

        assertEquals(3, count);
    }

    @Test
    void markAsRead_success() {
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationDTO result = notificationService.markAsRead(testNotification.getId(), testUser);

        assertTrue(result.isRead());
    }

    @Test
    void markAsRead_otherUser_forbidden() {
        User otherUser = User.builder()
                .id(UUID.randomUUID()).name("Other User").role(UserRole.SALES_REP).build();

        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));

        assertThrows(AccessDeniedException.class,
                () -> notificationService.markAsRead(testNotification.getId(), otherUser));
    }

    @Test
    void markAsRead_notFound_throws404() {
        UUID nonExistentId = UUID.randomUUID();
        when(notificationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(nonExistentId, testUser));
    }
}
