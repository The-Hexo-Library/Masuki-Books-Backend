package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "books_metadata", indexes = @Index(name = "idx_books_metadata_category", columnList = "category_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooksMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(length = 255)
    private String publisher;

    @Column(unique = true, length = 20, columnDefinition = "varchar(20)")
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(nullable = false, length = 20)
    private String format; // paperback, hardcover, ebook, audiobook

    private Integer pages;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false, length = 20)
    private String status; // active, inactive, draft

    // Digital content fields
    @Builder.Default
    @Column(name = "content_type", length = 20)
    private String contentType = "physical"; // physical, digital, both

    @Column(name = "file_key", length = 500)
    private String fileKey; // S3 key for the digital file (PDF/EPUB)

    @Column(name = "file_format", length = 20)
    private String fileFormat; // pdf, epub

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "total_pages")
    private Integer totalPages; // for digital reader pagination

    @Builder.Default
    @Column(name = "preview_pages")
    private Integer previewPages = 10; // number of free preview pages

    @Builder.Default
    @Column(name = "downloadable")
    private Boolean downloadable = false; // whether offline download is allowed

    @Builder.Default
    @Column(name = "max_downloads")
    private Integer maxDownloads = 3; // per-purchase download limit

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
