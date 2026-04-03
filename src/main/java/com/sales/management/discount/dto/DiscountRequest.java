package com.sales.management.discount.dto;

import com.sales.management.discount.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public class DiscountRequest {

    @NotBlank(message = "code is required")
    @Size(max = 32, message = "code must not exceed 32 characters")
    private String code;

    @NotNull(message = "type is required")
    private DiscountType type;

    @NotNull(message = "value is required")
    @DecimalMin(value = "0.01", message = "value must be positive")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "minOrderAmount must not be negative")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0", message = "maxDiscountAmount must not be negative")
    private BigDecimal maxDiscountAmount;

    private Boolean active = true;

    private Instant expiresAt;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DiscountType getType() { return type; }
    public void setType(DiscountType type) { this.type = type; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
