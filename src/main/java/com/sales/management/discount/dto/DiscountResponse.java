package com.sales.management.discount.dto;

import com.sales.management.discount.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountResponse(
        Long id,
        String code,
        DiscountType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Boolean active,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
