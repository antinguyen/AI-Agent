package com.sales.management.product;

import java.util.List;

public record ProductOptionsResponse(
        List<String> suppliers,
        List<String> brands,
        List<String> originCountries,
        List<String> categories,
        List<CurrencyRateOptionResponse> currencies
) {
}
