package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiscountCodeRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String type;  // percentage, fixed_amount, free_shipping

    @NotNull
    private BigDecimal value;

    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private Integer maxUses;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
}
