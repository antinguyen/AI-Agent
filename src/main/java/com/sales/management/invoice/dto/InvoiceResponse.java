package com.sales.management.invoice.dto;

import com.sales.management.order.dto.OrderItemResponse;
import com.sales.management.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerCode,
        String customerName,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        Instant issuedAt,
        List<OrderItemResponse> items
) {
}
