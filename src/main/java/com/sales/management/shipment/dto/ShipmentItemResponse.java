package com.sales.management.shipment.dto;

public record ShipmentItemResponse(
        Long shipmentItemId,
        Long orderItemId,
        Long productId,
        String productSku,
        String productName,
        Integer quantity
) {
}
