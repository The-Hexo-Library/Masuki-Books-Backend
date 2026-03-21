package com.masukibooks.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecentReadResponse {

    private UUID productId;
    private String title;
    private String author;
    private String coverImageUrl;
    private String fileFormat;
    private LocalDateTime lastReadAt;
    private Integer currentPage;
    private Integer totalPages;
    private BigDecimal percentage;
}
