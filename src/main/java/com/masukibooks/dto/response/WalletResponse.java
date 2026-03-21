package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletResponse {
    private String walletId;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
