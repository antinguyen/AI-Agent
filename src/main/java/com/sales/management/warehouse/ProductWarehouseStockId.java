package com.sales.management.warehouse;

import java.io.Serializable;
import java.util.Objects;

public class ProductWarehouseStockId implements Serializable {
    private Long product;
    private Long warehouse;

    public ProductWarehouseStockId() {
    }

    public ProductWarehouseStockId(Long product, Long warehouse) {
        this.product = product;
        this.warehouse = warehouse;
    }

    public Long getProduct() {
        return product;
    }

    public void setProduct(Long product) {
        this.product = product;
    }

    public Long getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Long warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductWarehouseStockId that = (ProductWarehouseStockId) o;
        return Objects.equals(product, that.product) &&
               Objects.equals(warehouse, that.warehouse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, warehouse);
    }
}
