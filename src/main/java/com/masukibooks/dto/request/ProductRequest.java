package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductRequest {
    @NotBlank
    private String categoryId;

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    @NotBlank
    private String sku;

    private String publisher;
    private String isbn;
    private String description;

    @NotBlank
    private String format;  // paperback, hardcover, ebook, audiobook

    private String language;
    private Integer pages;
    private LocalDate publicationDate;

    @NotNull
    private BigDecimal price;

    private BigDecimal compareAtPrice;

    private String status = "draft";

    // Digital content fields
    private String contentType = "physical";  // physical, digital, both
    private String fileFormat;  // pdf, epub
    private String fileUrl;
    private Long fileSizeBytes;
    private Integer totalPages;
    private Integer previewPages;
    private Boolean downloadable;
    private Integer maxDownloads;
}
