package com.masukibooks.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardStatsResponse {

    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private BigDecimal totalRevenue;
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long lowStockItems;
    private long pendingRefunds;
    private long pendingReviews;
}
