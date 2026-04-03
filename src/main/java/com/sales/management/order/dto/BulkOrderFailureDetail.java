package com.sales.management.order.dto;

public record BulkOrderFailureDetail(
        Long orderId,
        String orderStatus,
        String reason
) {
}
