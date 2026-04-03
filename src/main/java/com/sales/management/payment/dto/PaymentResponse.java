package com.sales.management.payment.dto;

import com.sales.management.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String paymentNumber,
        Long orderId,
        String orderNumber,
        BigDecimal amount,
        PaymentMethod method,
        String note,
        Instant paidAt
) {
}
