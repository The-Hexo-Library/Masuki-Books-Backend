package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = @Index(name = "idx_orders_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false, length = 20)
    private String status;
    // pending, confirmed, packed, shipped, delivered, cancelled, refunded

    @Builder.Default
    @Column(name = "order_type", length = 20)
    private String orderType = "physical"; // physical, digital, mixed

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    // @Column(name = "tax_amount", precision = 10, scale = 2)
    // private BigDecimal taxAmount;

    // @Column(name = "shipping_amount", precision = 10, scale = 2)
    // private BigDecimal shippingAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "shipping_address_id")
    // private Address shippingAddress;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "billing_address_id")
    // private Address billingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    // @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch =
    // FetchType.LAZY)
    // private List<Shipment> shipments;
}
