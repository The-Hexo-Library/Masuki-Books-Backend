package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "support_tickets",
       indexes = {
           @Index(name = "idx_support_tickets_user", columnList = "user_id"),
           @Index(name = "idx_support_tickets_status", columnList = "status")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private UUID ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;  // account, payment, order, technical, other

    @Column(nullable = false, length = 30)
    private String priority;  // low, medium, high, urgent

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "open";  // open, in_progress, resolved, closed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private AdminUser assignedTo;

    @Column(name = "admin_response", columnDefinition = "text")
    private String adminResponse;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order relatedOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
