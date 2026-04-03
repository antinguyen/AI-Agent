package com.sales.management.preference.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserPreferenceUpdateRequest(
        @NotBlank(message = "locale is required")
        @Pattern(regexp = "^[a-z]{2}(?:-[A-Z]{2})?$", message = "locale must be like vi or vi-VN")
        String locale,

        @NotBlank(message = "currencyCode is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be ISO-4217 (e.g. VND)")
        String currencyCode,

        @NotNull(message = "reducedMotion is required")
        Boolean reducedMotion,

        @NotBlank(message = "defaultLandingPage is required")
        @Pattern(regexp = "^/.*$", message = "defaultLandingPage must start with /")
        String defaultLandingPage,

        @NotNull(message = "tablePageSize is required")
        @Min(value = 5, message = "tablePageSize must be >= 5")
        @Max(value = 100, message = "tablePageSize must be <= 100")
        Integer tablePageSize,

        @NotBlank(message = "orderListPresetKey is required")
        @Pattern(regexp = "^(ALL|PENDING_CONFIRMATION|READY_TO_SHIP|PAID|RETURNED|CANCELLED|CUSTOM)$",
                message = "orderListPresetKey is invalid")
        String orderListPresetKey,

        @NotNull(message = "orderListStatusFilter is required")
        @Pattern(regexp = "^(|CREATED|CONFIRMED|PAID|RETURNED|CANCELLED)$",
                message = "orderListStatusFilter is invalid")
        String orderListStatusFilter,

        @NotBlank(message = "orderListFulfillmentFilter is required")
        @Pattern(regexp = "^(ALL|PENDING|READY_TO_SHIP|SHIPPED|SHIPMENT_CANCELLED|CANCELLED)$",
                message = "orderListFulfillmentFilter is invalid")
        String orderListFulfillmentFilter
) {
}
