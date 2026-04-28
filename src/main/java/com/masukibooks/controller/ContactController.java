package com.masukibooks.controller;

import com.masukibooks.dto.request.AdminReplyRequest;
import com.masukibooks.dto.request.ContactFormRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.ContactSubmissionResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUserId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Public endpoint — anyone (even unauthenticated) can submit a contact form.
     */
    @PostMapping("/api/contact")
    public ResponseEntity<ApiResponse<ContactSubmissionResponse>> submitContactForm(
            @Valid @RequestBody ContactFormRequest request,
            Authentication authentication) {

        UUID userId = getUserIdFromAuthentication(authentication);

        return ResponseEntity.ok(ApiResponse.success(
                "Contact form submitted successfully. We'll get back to you soon!",
                contactService.submitContactForm(request, userId)));
    }

    /**
     * Admin: list all contact submissions.
     */
    @GetMapping("/admin/contact-submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ContactSubmissionResponse>>> getSubmissions() {
        return ResponseEntity.ok(ApiResponse.success("Contact submissions retrieved", contactService.getAllSubmissions()));
    }

    /**
     * Admin: reply to a contact submission.
     */
    @PostMapping("/admin/contact-submissions/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContactSubmissionResponse>> replyToSubmission(
            @Valid @RequestBody AdminReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Reply sent successfully", contactService.replyToSubmission(request)));
    }

    /**
     * User: list their own contact submissions.
     */
    @GetMapping("/api/contact/my-submissions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ContactSubmissionResponse>>> getMySubmissions(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.success("Unauthorized", null));
        }
        return ResponseEntity.ok(ApiResponse.success("User submissions retrieved", contactService.getUserSubmissions(userId)));
    }
}
