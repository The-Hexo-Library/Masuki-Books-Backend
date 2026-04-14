package com.masukibooks.dto.response;

import com.masukibooks.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionResponse {
    private UUID subscriptionId;
    private String planName;
    private Integer accessPercentage;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Boolean isPlan;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Boolean autoRenew;
}
