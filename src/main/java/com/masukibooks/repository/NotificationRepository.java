package com.masukibooks.repository;

import com.masukibooks.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientRoleOrderByCreatedAtDesc(String recipientRole);
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);
    long countByRecipientRoleAndIsReadFalse(String recipientRole);
    long countByRecipientUserIdAndIsReadFalse(UUID recipientUserId);
}
