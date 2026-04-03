package com.sales.management.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalOrders,
        List<DailyRevenue> daily
) {
    public record DailyRevenue(
            LocalDate date,
            BigDecimal revenue,
            long orders
    ) {}
}
