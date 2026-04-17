package com.masukibooks.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibraryResponse {

    private UUID userLibraryId;
    private UUID productId;
    private String title;
    private String author;
    private String coverImageUrl;
    private String fileUrl;
    private String fileFormat;
    private String accessType;
    private LocalDateTime acquiredAt;
    private LocalDateTime expiresAt;
    private String status;

    // Reading progress
    private Integer currentPage;
    private Integer totalPages;
    private BigDecimal readingPercentage;
    private LocalDateTime lastReadAt;
}
