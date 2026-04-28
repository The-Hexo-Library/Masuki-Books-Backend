package com.masukibooks.service;

import com.masukibooks.dto.request.AdminReplyRequest;
import com.masukibooks.dto.request.ContactFormRequest;
import com.masukibooks.dto.response.ContactSubmissionResponse;
import com.masukibooks.entity.ContactSubmission;
import com.masukibooks.entity.Notification;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.ContactSubmissionRepository;
import com.masukibooks.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactSubmissionRepository contactRepo;
    private final NotificationRepository notificationRepo;
    private final JavaMailSender mailSender;

    private static final String[] ADMIN_EMAILS = {
            "Contactmasuki@masukibooks.com",
            "giriofkala2004@gmail.com"
    };

    @Transactional
    public ContactSubmissionResponse submitContactForm(ContactFormRequest request, UUID userId) {
        ContactSubmission submission = ContactSubmission.builder()
                .name(request.getName())
                .email(request.getEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status("PENDING")
                .userId(userId)
                .build();

        submission = contactRepo.save(submission);

        // Send email to admin addresses
        sendContactEmail(submission);

        // Create notification for admin
        Notification adminNotification = Notification.builder()
                .recipientRole("ADMIN")
                .type("CONTACT_FORM_SUBMITTED")
                .title("New Contact Form: " + submission.getSubject())
                .message(submission.getName() + " (" + submission.getEmail() + ") submitted a contact form: " + truncate(submission.getMessage(), 200))
                .referenceId(submission.getId())
                .isRead(false)
                .build();
        notificationRepo.save(adminNotification);

        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<ContactSubmissionResponse> getAllSubmissions() {
        return contactRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContactSubmissionResponse> getUserSubmissions(UUID userId) {
        return contactRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactSubmissionResponse replyToSubmission(AdminReplyRequest request) {
        ContactSubmission submission = contactRepo.findById(request.getContactSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found"));

        submission.setAdminReply(request.getReplyMessage());
        submission.setStatus("REPLIED");
        submission = contactRepo.save(submission);

        // Send reply email to the user
        sendReplyEmail(submission, request.getReplyMessage());

        // Create notification for the user (if they are registered)
        if (submission.getUserId() != null) {
            Notification userNotification = Notification.builder()
                    .recipientUserId(submission.getUserId())
                    .recipientRole("USER")
                    .type("ADMIN_REPLY")
                    .title("Reply to: " + submission.getSubject())
                    .message("Admin replied to your contact form: " + truncate(request.getReplyMessage(), 200))
                    .referenceId(submission.getId())
                    .isRead(false)
                    .build();
            notificationRepo.save(userNotification);
        }

        // Also create a notification by email for non-registered users
        // (the reply email itself serves this purpose)

        return toResponse(submission);
    }

    private void sendContactEmail(ContactSubmission submission) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(ADMIN_EMAILS);
            mail.setSubject("[Masuki Books Contact] " + submission.getSubject());
            mail.setText(
                    "New contact form submission:\n\n" +
                    "Name: " + submission.getName() + "\n" +
                    "Email: " + submission.getEmail() + "\n" +
                    "Subject: " + submission.getSubject() + "\n\n" +
                    "Message:\n" + submission.getMessage() + "\n\n" +
                    "---\n" +
                    "Submitted at: " + submission.getCreatedAt() + "\n" +
                    "Submission ID: " + submission.getId()
            );
            mail.setReplyTo(submission.getEmail());
            mailSender.send(mail);
            log.info("Contact form email sent for submission {}", submission.getId());
        } catch (Exception e) {
            log.error("Failed to send contact form email for submission {}: {}", submission.getId(), e.getMessage());
            // Don't throw — the submission is saved even if email fails
        }
    }

    private void sendReplyEmail(ContactSubmission submission, String replyMessage) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(submission.getEmail());
            mail.setSubject("Re: " + submission.getSubject() + " — Masuki Books Support");
            mail.setText(
                    "Dear " + submission.getName() + ",\n\n" +
                    "Thank you for contacting Masuki Books. Here is our response:\n\n" +
                    replyMessage + "\n\n" +
                    "---\n" +
                    "Original message:\n" +
                    submission.getMessage() + "\n\n" +
                    "Best regards,\n" +
                    "Masuki Books Support Team"
            );
            mailSender.send(mail);
            log.info("Reply email sent to {} for submission {}", submission.getEmail(), submission.getId());
        } catch (Exception e) {
            log.error("Failed to send reply email for submission {}: {}", submission.getId(), e.getMessage());
        }
    }

    private ContactSubmissionResponse toResponse(ContactSubmission s) {
        return ContactSubmissionResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .email(s.getEmail())
                .subject(s.getSubject())
                .message(s.getMessage())
                .status(s.getStatus())
                .adminReply(s.getAdminReply())
                .userId(s.getUserId())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
