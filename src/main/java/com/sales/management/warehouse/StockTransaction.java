package com.sales.management.warehouse;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_product_warehouse", columnList = "product_id,warehouse_id"),
        @Index(name = "idx_transaction_type", columnList = "transaction_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(length = 32)
    private String referenceType; // ORDER, PURCHASE_ORDER, STOCK_COUNT

    @Column
    private Long referenceId;

    @Column(length = 512)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 64)
    private String createdBy;

    public enum TransactionType {
        INITIAL, ADJUSTMENT, PURCHASE, SALES, RETURN, RESERVATION, RELEASE
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public StockTransaction() {
    }

    public StockTransaction(Long productId, Long warehouseId, TransactionType transactionType, 
                           Integer quantityChange, String referenceType, Long referenceId, String notes) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.transactionType = transactionType;
        this.quantityChange = quantityChange;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.notes = notes;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
