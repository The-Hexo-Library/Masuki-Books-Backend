package com.masukibooks.service;

import com.masukibooks.dto.response.DashboardStatsResponse;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final InventoryRepository inventoryRepository;
    private final RefundRepository refundRepository;
    private final ReviewRepository reviewRepository;

    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus("pending");
        long confirmedOrders = orderRepository.countByStatus("confirmed");
        long deliveredOrders = orderRepository.countByStatus("delivered");
        long cancelledOrders = orderRepository.countByStatus("cancelled");

        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus("success");
        long totalPayments = paymentRepository.count();
        long successfulPayments = paymentRepository.countByStatus("success");
        long failedPayments = paymentRepository.countByStatus("failed");

        long openTickets = supportTicketRepository.countByStatus("open");
        long inProgressTickets = supportTicketRepository.countByStatus("in_progress");
        long resolvedTickets = supportTicketRepository.countByStatus("resolved");

        long lowStockItems = inventoryRepository.countLowStock();
        long pendingRefunds = refundRepository.countByStatus("requested");
        long pendingReviews = reviewRepository.countByStatus("pending");

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalPayments(totalPayments)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .openTickets(openTickets)
                .inProgressTickets(inProgressTickets)
                .resolvedTickets(resolvedTickets)
                .lowStockItems(lowStockItems)
                .pendingRefunds(pendingRefunds)
                .pendingReviews(pendingReviews)
                .build();
    }
}
