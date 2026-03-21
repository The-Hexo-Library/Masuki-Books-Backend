package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicketRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be 200 characters or less")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;  // account, payment, order, technical, other

    private String priority;  // low, medium, high, urgent (defaults to medium)

    private UUID orderId;  // optional, links ticket to an order
}
