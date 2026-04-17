package com.masukibooks.dto.response;

import com.masukibooks.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionStatusResponse {
    private boolean active;
    private String planName;
    private Integer accessPercentage;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Long totalPublicBooks;
    private Long usedBooks;
    private Long allowedBooks;
    private boolean limitExceeded;
}
