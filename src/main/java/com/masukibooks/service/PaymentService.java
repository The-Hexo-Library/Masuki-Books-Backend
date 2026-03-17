package com.masukibooks.service;

import com.masukibooks.dto.request.PaymentCallbackRequest;
import com.masukibooks.entity.Order;
import com.masukibooks.entity.OrderItem;
import com.masukibooks.entity.Payment;
import com.masukibooks.entity.Product;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.OrderRepository;
import com.masukibooks.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserLibraryService userLibraryService;

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

            // Auto-unlock digital books in user library
            unlockDigitalContent(order);
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

    private void unlockDigitalContent(Order order) {
        if (order.getUser() == null) return;

        String orderType = order.getOrderType();
        if (!"digital".equals(orderType) && !"mixed".equals(orderType)) return;

        if (order.getItems() == null) return;

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if ("digital".equals(product.getContentType()) || "both".equals(product.getContentType())) {
                try {
                    userLibraryService.addToLibrary(
                            order.getUser().getUserId(),
                            product.getProductId(),
                            "purchased",
                            order.getOrderId()
                    );
                    log.info("Unlocked digital book {} for user {} via order {}",
                            product.getProductId(), order.getUser().getUserId(), order.getOrderId());
                } catch (Exception e) {
                    log.error("Failed to unlock digital book {} for user {}: {}",
                            product.getProductId(), order.getUser().getUserId(), e.getMessage());
                }
            }
        }
    }
}
