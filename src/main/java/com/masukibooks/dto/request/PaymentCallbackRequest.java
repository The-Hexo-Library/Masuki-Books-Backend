package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentCallbackRequest {
    @NotBlank
    private String orderNumber;

    @NotBlank
    private String gatewayTransactionId;

    @NotBlank
    private String gatewayPaymentId;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String status;  // success, failed

    private String failureReason;
}
