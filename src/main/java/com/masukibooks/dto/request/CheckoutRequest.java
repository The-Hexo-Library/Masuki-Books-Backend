package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;
import java.util.UUID;

@Data
public class CheckoutRequest {
    private UUID shippingAddressId; // nullable for digital orders

    private UUID billingAddressId; // null = same as shipping

    @NotBlank
    private String paymentMethod; // card, upi, net_banking, wallet

    @NotBlank
    private String gateway;

    private String discountCode;

    private String guestEmail; // for guest checkout

    @NotBlank
    private String currency;
}
