package com.sales.management.order.dto;

import com.sales.management.order.OrderStatus;
import com.sales.management.order.OrderFulfillmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerCode,
        String customerName,
        OrderStatus status,
        OrderFulfillmentStatus fulfillmentStatus,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String discountCode,
        Instant createdAt,
        List<OrderItemResponse> items,
        String createdBy
) {
}
