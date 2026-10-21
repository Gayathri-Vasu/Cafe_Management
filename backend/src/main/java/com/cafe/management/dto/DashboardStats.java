package com.cafe.management.dto;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated statistics shown on the admin dashboard. */
public record DashboardStats(
        long totalCategories,
        long totalProducts,
        long totalOrders,
        long totalBills,
        BigDecimal totalRevenue,
        BigDecimal todayRevenue,
        long pendingBills,
        long paidBills,
        List<OrderResponse> recentOrders
) {
}
