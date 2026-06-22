package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.NotificationResponse;
import com.masukibooks.entity.User;
import com.masukibooks.entity.UserRole;
import com.masukibooks.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal User user,
            Authentication authentication) {
        if (isAdmin(user, authentication)) {
            return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getNotificationsForAdmin()));
        }
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getNotificationsForUser(user.getUserId())));
    }

    /**
     * Get the count of unread notifications.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user,
            Authentication authentication) {
        long count;
        if (isAdmin(user, authentication)) {
            count = notificationService.getUnreadCountForAdmin();
        } else {
            count = notificationService.getUnreadCountForUser(user.getUserId());
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

    private boolean isAdmin(User user, Authentication authentication) {
        if (user != null && UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_SUPER_ADMIN"));
    }
}
