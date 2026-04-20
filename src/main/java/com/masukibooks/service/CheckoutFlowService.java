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
    private final SubscriptionService subscriptionService;

    @Transactional
    public CheckoutFlowResponse checkoutAndInitiate(UUID userId, UserCheckoutRequest request) {
        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setDiscountCode(request.getDiscountCode());
        checkoutRequest.setCurrency(request.getCurrency());

        // First create the order to get the total amount
        OrderResponse order = orderService.checkout(userId, null, checkoutRequest);

        BigDecimal orderTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal walletUsed = BigDecimal.ZERO;
        BigDecimal walletBalanceAfter = BigDecimal.ZERO;

        // Apply wallet if requested
        if (request.isUseWallet() && userId != null) {
            BigDecimal walletBalance = subscriptionService.getWalletBalance(userId);
            if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
                walletUsed = subscriptionService.deductFromWallet(userId, orderTotal);
                walletBalanceAfter = subscriptionService.getWalletBalance(userId);

                // Update the order's discount amount and total to reflect wallet usage
                BigDecimal newTotal = orderTotal.subtract(walletUsed);
                if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
                    newTotal = BigDecimal.ZERO;
                }
                orderService.updateOrderWalletDeduction(order.getOrderId(), walletUsed, newTotal);
                order = orderService.getOrder(order.getOrderId());
            }
        }

        // Initiate payment for the remaining amount
        Payment payment = paymentService.initiatePayment(order.getOrderId(), request.getGateway(), request.getPaymentMethod());

        // In the demo flow, treat checkout as paid immediately so purchased books
        // are unlocked in the private library right away.
        String gateway = request.getGateway() == null ? "" : request.getGateway().trim().toLowerCase();
        if (gateway.isBlank() || "demo".equals(gateway)) {
            String txId = "demo-" + System.currentTimeMillis();
            payment = paymentService.markPaymentSuccess(order.getOrderId(), txId);
            order = orderService.getOrder(order.getOrderId());
        }

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
                .walletAmountUsed(walletUsed)
                .walletBalanceAfter(walletBalanceAfter)
                .build();
    }
}
