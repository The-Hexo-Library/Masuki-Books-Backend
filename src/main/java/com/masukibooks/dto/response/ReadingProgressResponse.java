package com.masukibooks.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReadingProgressResponse {

    private Integer currentPage;
    private Integer totalPages;
    private BigDecimal percentage;
    private LocalDateTime lastReadAt;
    private Long readingTimeSeconds;
}
