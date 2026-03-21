package com.masukibooks.dto.response;

import com.masukibooks.entity.Payment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckoutFlowResponse {
    private OrderResponse order;
    private Payment payment;
}
