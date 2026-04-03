package com.sales.management.payment.dto;

import com.sales.management.payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PaymentRequest {

    @NotNull(message = "method is required")
    private PaymentMethod method;

    @Size(max = 500, message = "note max length is 500")
    private String note;

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
