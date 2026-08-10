package com.masukibooks.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RazorpayCreateOrderRequest {

    @Min(100)
    private long amount; // paise

    private String currency = "INR"; // always INR for Razorpay

    @NotBlank
    private String receipt;

    /**
     * Internal order id so we can mark payment success against our Order after
     * verification.
     * Razorpay's order id will be returned separately.
     */
    private String internalOrderId;

    private String customerEmail;
}
