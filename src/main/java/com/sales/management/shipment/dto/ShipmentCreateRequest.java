package com.sales.management.shipment.dto;

import jakarta.validation.constraints.NotNull;

public class ShipmentCreateRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    private String note;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
