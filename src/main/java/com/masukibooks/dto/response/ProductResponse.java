package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class ProductResponse {
    private UUID productId;
    private String categoryId;
    private String categoryName;
    private String sku;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private String description;
    private String language;
    private String format;
    private Integer pages;
    private LocalDate publicationDate;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String status;
    private Integer stockQuantity;
    private boolean inStock;
    private Double averageRating;
    private List<String> imageUrls;
    private LocalDateTime createdAt;

    // Digital content fields
    private String contentType;
    private String fileKey;
    private String fileUrl;
    private String fileFormat;
    private Long fileSizeBytes;
    private Integer totalPages;
    private Integer previewPages;
    private boolean downloadable;
    private Integer maxDownloads;
}
