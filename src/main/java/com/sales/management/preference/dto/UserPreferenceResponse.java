package com.sales.management.preference.dto;

import com.sales.management.preference.UserPreference;

import java.time.Instant;

public record UserPreferenceResponse(
        String locale,
        String currencyCode,
        boolean reducedMotion,
        String defaultLandingPage,
        int tablePageSize,
    String orderListPresetKey,
    String orderListStatusFilter,
    String orderListFulfillmentFilter,
        Instant updatedAt
) {
    public static UserPreferenceResponse from(UserPreference p) {
        return new UserPreferenceResponse(
                p.getLocale(),
                p.getCurrencyCode(),
                p.isReducedMotion(),
                p.getDefaultLandingPage(),
                p.getTablePageSize(),
        p.getOrderListPresetKey(),
        p.getOrderListStatusFilter(),
        p.getOrderListFulfillmentFilter(),
                p.getUpdatedAt()
        );
    }
}
