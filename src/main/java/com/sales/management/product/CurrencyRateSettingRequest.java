package com.sales.management.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CurrencyRateSettingRequest(
        @NotBlank(message = "currencyCode is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be ISO-4217 format")
        String currencyCode,

        @NotBlank(message = "bankName is required")
        @Size(max = 128, message = "bankName max length is 128")
        String bankName,

        @DecimalMin(value = "0.000001", inclusive = true, message = "rateToVnd must be greater than 0")
        BigDecimal rateToVnd
) {
}
