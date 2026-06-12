package com.masukibooks.service;

import com.masukibooks.dto.request.CheckoutRequest;
import com.masukibooks.dto.request.UserCheckoutRequest;
import com.masukibooks.dto.response.CheckoutFlowResponse;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.entity.Payment;
import com.masukibooks.service.RazorpayPaymentService.RazorpayCheckoutDetails;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutFlowService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final RazorpayPaymentService razorpayPaymentService;
    private final com.masukibooks.repository.OrderRepository orderRepository;

    @Transactional
    public CheckoutFlowResponse checkoutAndInitiate(UUID userId, String guestToken, UserCheckoutRequest request) {
        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setDiscountCode(request.getDiscountCode());
        checkoutRequest.setCurrency(request.getCurrency());

        OrderResponse order = orderService.checkout(userId, guestToken, checkoutRequest);
        com.masukibooks.entity.Order orderEntity = orderRepository.findById(order.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order entity not found for id " + order.getOrderId()));

        RazorpayCheckoutDetails razorpayCheckout;
        try {
            razorpayCheckout = razorpayPaymentService.createOrderForCheckout(orderEntity);
        } catch (RazorpayException ex) {
            throw new RuntimeException("Failed to create Razorpay order: " + ex.getMessage(), ex);
        }

        Payment payment = paymentService.getPaymentByOrder(order.getOrderId());

        CheckoutFlowResponse.PaymentSummary paymentSummary = CheckoutFlowResponse.PaymentSummary.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : order.getOrderId())
                .gateway(payment.getGateway())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .build();

        return CheckoutFlowResponse.builder()
                .order(order)
                .payment(paymentSummary)
                .publishableKey(razorpayCheckout.keyId())
                .razorpayOrderId(razorpayCheckout.razorpayOrderId())
                .amount(razorpayCheckout.amount())
                .currency(razorpayCheckout.currency())
                .build();
    }
}
