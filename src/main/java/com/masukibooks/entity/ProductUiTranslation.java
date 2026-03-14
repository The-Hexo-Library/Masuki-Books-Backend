package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_ui_translations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "language_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductUiTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "translation_id")
    private UUID translationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
