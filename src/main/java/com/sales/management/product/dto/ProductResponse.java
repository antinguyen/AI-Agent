package com.sales.management.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        BigDecimal purchasePrice,
        String unit,
        String currencyCode,
        BigDecimal exchangeRate,
        String imageUrl,
        String supplier,
        String brand,
        String originCountry,
        String category,
        BigDecimal vatRate,
        Integer manufactureYear,
        Integer stockQuantity,
        Integer lowStockThreshold,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
