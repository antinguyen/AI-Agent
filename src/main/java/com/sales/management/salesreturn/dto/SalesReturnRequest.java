package com.sales.management.salesreturn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SalesReturnRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason max length is 500")
    private String reason;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<SalesReturnItemRequest> items;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<SalesReturnItemRequest> getItems() { return items; }
    public void setItems(List<SalesReturnItemRequest> items) { this.items = items; }
}
