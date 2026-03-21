package com.masukibooks.service;

import com.masukibooks.dto.request.CheckoutRequest;
import com.masukibooks.dto.request.UserCheckoutRequest;
import com.masukibooks.dto.response.CheckoutFlowResponse;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutFlowService {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Transactional
    public CheckoutFlowResponse checkoutAndInitiate(UUID userId, UserCheckoutRequest request) {
        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setDiscountCode(request.getDiscountCode());
        checkoutRequest.setCurrency(request.getCurrency());

        OrderResponse order = orderService.checkout(userId, null, checkoutRequest);
        Payment payment = paymentService.initiatePayment(order.getOrderId(), request.getGateway(), request.getPaymentMethod());

        return CheckoutFlowResponse.builder()
                .order(order)
                .payment(payment)
                .build();
    }
}
