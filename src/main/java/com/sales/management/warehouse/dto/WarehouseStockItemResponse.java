package com.sales.management.warehouse.dto;

public class WarehouseStockItemResponse {
    public final Long productId;
    public final String productSku;
    public final String productName;
    public final Integer quantity;
    public final Integer reserved;
    public final Integer available;
    public final Integer threshold;
    public final Boolean lowStock;

    public WarehouseStockItemResponse(Long productId, String productSku, String productName,
                                      Integer quantity, Integer reserved, Integer available,
                                      Integer threshold, Boolean lowStock) {
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.quantity = quantity;
        this.reserved = reserved;
        this.available = available;
        this.threshold = threshold;
        this.lowStock = lowStock;
    }
}
