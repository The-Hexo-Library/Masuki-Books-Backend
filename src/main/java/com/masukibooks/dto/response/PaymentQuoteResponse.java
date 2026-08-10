package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentQuoteResponse {
    private BigDecimal usdAmount;
    private BigDecimal exchangeRate;
    private String currency;
    private BigDecimal convertedAmount;
    private long amountPaise;
}