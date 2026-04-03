package com.sales.management.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CurrencyRateSettingsUpdateRequest(
        @NotEmpty(message = "rates must not be empty")
        List<@Valid CurrencyRateSettingRequest> rates
) {
}
