package com.masukibooks.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicketResponse {

    private UUID ticketId;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String subject;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String adminResponse;
    private UUID assignedToId;
    private String assignedToName;
    private UUID orderId;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
