package com.masukibooks.service;

import com.masukibooks.entity.Notification;
import com.masukibooks.entity.User;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.NotificationRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public Notification createNotification(UUID userId, String channel, String subject, String body) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        Notification notification = Notification.builder()
                .user(user)
                .channel(channel)
                .subject(subject)
                .body(body)
                .status("queued")
                .build();
        notification = notificationRepository.save(notification);

        if ("email".equals(channel) && user != null) {
            sendEmail(user.getEmail(), subject, body);
            notification.setStatus("sent");
            notification.setSentAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return notification;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setStatus("read");
            notificationRepository.save(n);
        });
    }
}
