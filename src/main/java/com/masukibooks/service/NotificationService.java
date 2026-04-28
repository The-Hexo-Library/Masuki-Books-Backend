package com.masukibooks.service;

import com.masukibooks.dto.response.NotificationResponse;
import com.masukibooks.entity.Notification;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForAdmin() {
        return notificationRepo.findByRecipientRoleOrderByCreatedAtDesc("ADMIN").stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(UUID userId) {
        return notificationRepo.findByRecipientUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForAdmin() {
        return notificationRepo.countByRecipientRoleAndIsReadFalse("ADMIN");
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(UUID userId) {
        return notificationRepo.countByRecipientUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notification = notificationRepo.save(notification);
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientUserId(n.getRecipientUserId())
                .recipientRole(n.getRecipientRole())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
