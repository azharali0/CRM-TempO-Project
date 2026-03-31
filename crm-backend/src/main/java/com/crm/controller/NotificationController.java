package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.NotificationDTO;
import com.crm.model.entity.User;
import com.crm.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        Page<NotificationDTO> notifications = notificationService.getMyNotifications(
                currentUser, PageRequest.of(page, Math.min(size, 100)));

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved",
                Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        NotificationDTO notification = notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notification));
    }
}
