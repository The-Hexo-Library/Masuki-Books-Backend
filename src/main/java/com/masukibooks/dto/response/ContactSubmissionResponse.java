package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ContactSubmissionResponse {
    private UUID id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private String status;
    private String adminReply;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
