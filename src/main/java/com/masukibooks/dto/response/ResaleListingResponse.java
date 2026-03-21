package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ResaleListingResponse {
    private String resaleId;
    private String sellerId;
    private String sellerName;
    private String productId;
    private String title;
    private String author;
    private String coverImageUrl;
    private BigDecimal originalPrice;
    private BigDecimal listingPrice;
    private Long readingTimeSeconds;
    private String status;
    private LocalDateTime listedAt;
    private LocalDateTime soldAt;
}
