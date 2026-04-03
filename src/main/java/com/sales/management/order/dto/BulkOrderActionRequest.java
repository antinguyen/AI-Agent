package com.sales.management.order.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BulkOrderActionRequest {

    @NotEmpty(message = "orderIds is required")
    private List<Long> orderIds;

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }
}
