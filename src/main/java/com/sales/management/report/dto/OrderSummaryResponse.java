package com.sales.management.report.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrderSummaryResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        Map<String, Long> countByStatus
) {}
