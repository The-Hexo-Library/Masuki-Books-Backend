package com.masukibooks.service;

import com.masukibooks.entity.Order;
import com.masukibooks.entity.Refund;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.OrderRepository;
import com.masukibooks.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Refund requestRefund(UUID orderId, BigDecimal amount, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!java.util.List.of("paid", "delivered", "processing").contains(order.getStatus())) {
            throw new BusinessException("Order is not eligible for a refund");
        }
        Refund refund = Refund.builder()
                .order(order)
                .amount(amount)
                .reason(reason)
                .status("pending")
                .build();
        return refundRepository.save(refund);
    }

    @Transactional
    public Refund processRefund(UUID refundId, String status) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        refund.setStatus(status);
        if ("approved".equals(status)) {
            Order order = refund.getOrder();
            order.setStatus("refunded");
            orderRepository.save(order);
        }
        return refundRepository.save(refund);
    }

    public Page<Refund> getRefundsByStatus(String status, Pageable pageable) {
        return refundRepository.findByStatus(status, pageable);
    }
}
