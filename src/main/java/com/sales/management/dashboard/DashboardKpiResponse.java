package com.sales.management.dashboard;

import java.math.BigDecimal;

public record DashboardKpiResponse(
        BigDecimal todayRevenue,
        long todayOrderCount,
        long pendingOrderCount,
        long lowStockCount,
        long activeCustomerCount
) {
}
