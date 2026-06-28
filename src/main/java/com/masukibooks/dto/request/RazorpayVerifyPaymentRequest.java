package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RazorpayVerifyPaymentRequest {

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;

    /**
     * Internal Order UUID created by the app (from /user/checkout).
     * Used to mark payment success/unlock content after signature verification.
     */
    @NotBlank
    private String internalOrderId;
}
