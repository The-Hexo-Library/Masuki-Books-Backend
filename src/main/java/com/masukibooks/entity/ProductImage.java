// package com.masukibooks.entity;

// import jakarta.persistence.*;
// import lombok.*;
// import org.hibernate.annotations.CreationTimestamp;

// import java.time.LocalDateTime;
// import java.util.UUID;

// @Entity
// @Table(name = "product_images")
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// public class ProductImage {

// @Id
// @GeneratedValue(strategy = GenerationType.UUID)
// @Column(name = "image_id")
// private UUID imageId;

// @ManyToOne(fetch = FetchType.LAZY)
// @JoinColumn(name = "product_id", nullable = false)
// private Product product;

// @Column(nullable = false, length = 500)
// private String url;

// @Column(name = "alt_text", length = 255)
// private String altText;

// @Column(name = "display_order", nullable = false)
// private Integer displayOrder;

// @Column(name = "is_primary", nullable = false)
// private Boolean isPrimary;

// @CreationTimestamp
// @Column(name = "created_at", nullable = false, updatable = false)
// private LocalDateTime createdAt;
// }
