package com.sales.management.warehouse;

import com.sales.management.product.Product;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "product_warehouse_stock")
@IdClass(ProductWarehouseStockId.class)
public class ProductWarehouseStock {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private Integer lowStockThreshold = 10;

    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Column
    private Instant lastCountAt;

    public ProductWarehouseStock() {
    }

    public ProductWarehouseStock(Product product, Warehouse warehouse, Integer quantity, Integer lowStockThreshold) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.reservedQuantity = 0;
    }

    public Integer getAvailableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }

    public void reserve(Integer amount) {
        if (amount > getAvailableQuantity()) {
            throw new IllegalArgumentException("Cannot reserve more than available quantity");
        }
        this.reservedQuantity += amount;
    }

    public void release(Integer amount) {
        this.reservedQuantity = Math.max(0, reservedQuantity - amount);
    }

    public void deduct(Integer amount) {
        this.quantity = Math.max(0, quantity - amount);
        this.reservedQuantity = Math.max(0, reservedQuantity - amount);
    }

    public void add(Integer amount) {
        this.quantity += amount;
    }

    // Getters & Setters
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Instant getLastCountAt() {
        return lastCountAt;
    }

    public void setLastCountAt(Instant lastCountAt) {
        this.lastCountAt = lastCountAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductWarehouseStock that = (ProductWarehouseStock) o;
        return java.util.Objects.equals(product, that.product) &&
               java.util.Objects.equals(warehouse, that.warehouse);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(product, warehouse);
    }
}
