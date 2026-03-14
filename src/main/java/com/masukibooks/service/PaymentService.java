package com.masukibooks.service;

import com.masukibooks.dto.request.PaymentCallbackRequest;
import com.masukibooks.entity.Order;
import com.masukibooks.entity.Payment;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.OrderRepository;
import com.masukibooks.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Payment initiatePayment(UUID orderId, String gateway, String method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Payment payment = Payment.builder()
                .order(order)
                .gateway(gateway)
                .paymentMethod(method)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status("pending")
                .build();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment handleCallback(PaymentCallbackRequest request) {
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Payment payment = paymentRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(order.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setGatewayTransactionId(request.getGatewayTransactionId());
        payment.setStatus(request.getStatus());

        if ("completed".equals(request.getStatus())) {
            payment.setStatus("success");
            order.setStatus("confirmed");
        } else if ("failed".equals(request.getStatus())) {
            payment.setStatus("failed");
            payment.setFailureReason(request.getFailureReason());
        }

        orderRepository.save(order);
        return paymentRepository.save(payment);
    }

    public Payment getPaymentByOrder(UUID orderId) {
        return paymentRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
    }
}
