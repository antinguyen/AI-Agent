package com.sales.management.salesreturn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SalesReturnItemRequest {

    @NotNull(message = "orderItemId is required")
    private Long orderItemId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
