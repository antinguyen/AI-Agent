package com.sales.management.warehouse.dto;

import java.util.List;

public class ProductStockByWarehouseResponse {
    public final Long productId;
    public final String productSku;
    public final String productName;
    public final List<WarehouseStockDetail> warehouseStock;
    public final Integer totalQuantity;
    public final Integer totalReserved;
    public final Integer totalAvailable;

    public ProductStockByWarehouseResponse(Long productId, String productSku, String productName,
                                         List<WarehouseStockDetail> warehouseStock,
                                         Integer totalQuantity, Integer totalReserved, Integer totalAvailable) {
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.warehouseStock = warehouseStock;
        this.totalQuantity = totalQuantity;
        this.totalReserved = totalReserved;
        this.totalAvailable = totalAvailable;
    }

    public static class WarehouseStockDetail {
        public final Long warehouseId;
        public final String warehouseName;
        public final Integer quantity;
        public final Integer reserved;
        public final Integer available;
        public final Integer threshold;
        public final Boolean lowStock;

        public WarehouseStockDetail(Long warehouseId, String warehouseName, Integer quantity, 
                                   Integer reserved, Integer available, Integer threshold, Boolean lowStock) {
            this.warehouseId = warehouseId;
            this.warehouseName = warehouseName;
            this.quantity = quantity;
            this.reserved = reserved;
            this.available = available;
            this.threshold = threshold;
            this.lowStock = lowStock;
        }
    }
}
