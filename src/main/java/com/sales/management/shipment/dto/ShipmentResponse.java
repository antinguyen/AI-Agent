package com.sales.management.shipment.dto;

import com.sales.management.shipment.ShipmentStatus;

import java.time.Instant;
import java.util.List;

public record ShipmentResponse(
        Long id,
        String shipmentNumber,
        Long orderId,
        String orderNumber,
        Long warehouseId,
        String warehouseName,
        ShipmentStatus status,
        String note,
        Instant shippedAt,
        Instant createdAt,
        List<ShipmentItemResponse> items
) {
}
