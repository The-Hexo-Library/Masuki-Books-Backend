package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class CheckoutFlowResponse {
    private OrderResponse order;
    private PaymentSummary payment;
    private String publishableKey;
    private String razorpayOrderId;
    private Long amount;
    private String currency;

    @Getter
    @Builder
    public static class PaymentSummary {
        private UUID paymentId;
        private UUID orderId;
        private String gateway;
        private String paymentMethod;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String gatewayTransactionId;
    }
}
