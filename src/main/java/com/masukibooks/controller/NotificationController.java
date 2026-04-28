package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.NotificationResponse;
import com.masukibooks.entity.UserRole;
import com.masukibooks.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get notifications for the authenticated user.
     * Admin users see admin notifications; regular users see their own.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(Authentication authentication) {
        if (isAdmin(authentication)) {
            return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getNotificationsForAdmin()));
        }
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getNotificationsForUser(userId)));
    }

    /**
     * Get the count of unread notifications.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        long count;
        if (isAdmin(authentication)) {
            count = notificationService.getUnreadCountForAdmin();
        } else {
            UUID userId = extractUserId(authentication);
            count = notificationService.getUnreadCountForUser(userId);
        }
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of("count", count)));
    }

    /**
     * Mark a notification as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markAsRead(id)));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_SUPER_ADMIN"));
    }

    private UUID extractUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
