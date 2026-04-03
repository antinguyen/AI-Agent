package com.sales.management.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderCreateRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    private Long warehouseId; // Optional, when omitted system uses global product stock

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<OrderCreateItemRequest> items;

    private String discountCode;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<OrderCreateItemRequest> getItems() {
        return items;
    }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }

    public void setItems(List<OrderCreateItemRequest> items) {
        this.items = items;
    }
}
