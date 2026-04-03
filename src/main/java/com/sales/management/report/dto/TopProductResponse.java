package com.sales.management.report.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String sku,
        String name,
        long totalQuantitySold,
        BigDecimal totalRevenue
) {}
