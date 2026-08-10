package com.masukibooks.service;

import com.masukibooks.dto.request.CheckoutRequest;
import com.masukibooks.dto.request.UserCheckoutRequest;
import com.masukibooks.dto.response.CheckoutFlowResponse;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutFlowService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PaymentQuoteService paymentQuoteService;

    @Transactional
    public CheckoutFlowResponse checkoutAndInitiate(UUID userId, UserCheckoutRequest request) {
        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setDiscountCode(request.getDiscountCode());

        String gateway = request.getGateway() == null ? "" : request.getGateway().trim().toLowerCase();
        if (gateway.isBlank() || "demo".equals(gateway)) {
            checkoutRequest.setCurrency("USD");
            OrderResponse order = orderService.checkout(userId, null, checkoutRequest, "USD", BigDecimal.ONE);
            Payment payment = paymentService.initiatePayment(order.getOrderId(), request.getGateway(), request.getPaymentMethod());

            String txId = "demo-" + System.currentTimeMillis();
            payment = paymentService.markPaymentSuccess(order.getOrderId(), txId);
            order = orderService.getOrder(order.getOrderId());

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
                    .build();
        }

        String resolvedCurrency = paymentQuoteService.resolveCurrency(gateway);
        BigDecimal exchangeRate = paymentQuoteService.resolveExchangeRate(gateway);
        checkoutRequest.setCurrency(resolvedCurrency);

        OrderResponse order = orderService.checkout(userId, null, checkoutRequest, resolvedCurrency, exchangeRate);
        Payment payment = paymentService.initiatePayment(order.getOrderId(), request.getGateway(), request.getPaymentMethod());

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
                .build();
    }
}
