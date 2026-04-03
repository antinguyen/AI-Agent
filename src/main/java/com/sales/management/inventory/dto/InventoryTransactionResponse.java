package com.sales.management.inventory.dto;

import com.sales.management.inventory.InventoryTransactionType;

import java.time.Instant;

public record InventoryTransactionResponse(
        Long id,
        Long productId,
        String productSku,
        Long orderId,
        InventoryTransactionType type,
        Integer quantity,
        String reason,
        Instant createdAt
) {
}
