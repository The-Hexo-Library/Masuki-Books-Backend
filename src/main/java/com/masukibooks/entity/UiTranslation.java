package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ui_translations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"language_code", "key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UiTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "translation_id")
    private UUID translationId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(nullable = false, length = 255)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
