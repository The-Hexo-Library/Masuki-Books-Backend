package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WalletResponse {
    private BigDecimal balance;
    private BigDecimal amountAdded;
    private String message;
}
