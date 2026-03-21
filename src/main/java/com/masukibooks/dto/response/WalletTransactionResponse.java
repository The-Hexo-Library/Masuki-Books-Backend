package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletTransactionResponse {
    private String transactionId;
    private String txnType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceType;
    private String referenceId;
    private LocalDateTime createdAt;
}
