package com.sales.management.product;

import java.math.BigDecimal;
import java.time.Instant;

public record CurrencyRateAuditLogResponse(
        Long id,
        String currencyCode,
        String oldBankName,
        String newBankName,
        BigDecimal oldRate,
        BigDecimal newRate,
        String action,
        String changedBy,
        Instant changedAt
) {
    static CurrencyRateAuditLogResponse from(CurrencyRateAuditLog log) {
        return new CurrencyRateAuditLogResponse(
                log.getId(),
                log.getCurrencyCode(),
                log.getOldBankName(),
                log.getNewBankName(),
                log.getOldRate(),
                log.getNewRate(),
                log.getAction(),
                log.getChangedBy(),
                log.getChangedAt()
        );
    }
}
