package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCheckoutRequest {

    private String discountCode;
    private String currency;

    @NotBlank
    private String gateway;

    @NotBlank
    private String paymentMethod;
}
