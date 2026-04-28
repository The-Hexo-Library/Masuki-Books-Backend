package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID recipientUserId;
    private String recipientRole;
    private String type;
    private String title;
    private String message;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
