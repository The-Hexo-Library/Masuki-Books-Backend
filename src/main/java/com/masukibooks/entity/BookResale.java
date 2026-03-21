package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "book_resales",
       indexes = {
           @Index(name = "idx_resale_seller", columnList = "seller_id"),
           @Index(name = "idx_resale_status", columnList = "status"),
           @Index(name = "idx_resale_product", columnList = "product_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookResale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resale_id")
    private UUID resaleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_library_id", nullable = false)
    private UserLibrary userLibrary;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "listing_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal listingPrice;

    @Column(name = "reading_time_seconds")
    private Long readingTimeSeconds;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "listed";  // listed, sold, cancelled

    @Column(name = "listed_at", nullable = false)
    private LocalDateTime listedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
