package com.sales.management.product;

import java.math.BigDecimal;
import java.time.Instant;

public record CurrencyRateOptionResponse(
        String currencyCode,
        String bankName,
        BigDecimal rateToVnd,
        Instant updatedAt
) {
}
