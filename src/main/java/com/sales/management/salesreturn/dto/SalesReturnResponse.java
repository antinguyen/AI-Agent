package com.sales.management.salesreturn.dto;

import com.sales.management.salesreturn.SalesReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesReturnResponse(
        Long id,
        String returnNumber,
        Long orderId,
        String orderNumber,
        String reason,
        SalesReturnStatus status,
        BigDecimal totalRefund,
        Instant createdAt,
        List<SalesReturnItemResponse> items
) {
}
