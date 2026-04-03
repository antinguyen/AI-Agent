package com.sales.management.preference;

import com.sales.management.auth.AppUser;
import com.sales.management.auth.UserRepository;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.preference.dto.UserPreferenceResponse;
import com.sales.management.preference.dto.UserPreferenceUpdateRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserPreferenceService {

    private static final Set<String> ALLOWED_LANDING_PAGES = Set.of(
            "/",
            "/orders",
            "/products",
            "/customers",
            "/shipments",
            "/finance",
            "/returns",
            "/releases",
            "/settings/preferences",
            "/settings/currency-rates",
            "/reports",
            "/users",
            "/warehouses",
            "/products/low-stock"
    );

            private static final Set<String> ALLOWED_ORDER_LIST_PRESETS = Set.of(
                "ALL",
                "PENDING_CONFIRMATION",
                "READY_TO_SHIP",
                "PAID",
                "RETURNED",
                "CANCELLED",
                "CUSTOM"
            );

            private static final Set<String> ALLOWED_ORDER_STATUS_FILTERS = Set.of(
                "",
                "CREATED",
                "CONFIRMED",
                "PAID",
                "RETURNED",
                "CANCELLED"
            );

            private static final Set<String> ALLOWED_ORDER_FULFILLMENT_FILTERS = Set.of(
                "ALL",
                "PENDING",
                "READY_TO_SHIP",
                "SHIPPED",
                "SHIPMENT_CANCELLED",
                "CANCELLED"
            );

    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public UserPreferenceService(UserPreferenceRepository preferenceRepository, UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserPreferenceResponse getMine() {
        AppUser user = requireCurrentUser();
        UserPreference pref = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user));
        return UserPreferenceResponse.from(pref);
    }

    @Transactional
    public UserPreferenceResponse updateMine(UserPreferenceUpdateRequest request) {
        AppUser user = requireCurrentUser();
        UserPreference pref = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user));

        String normalizedLanding = normalizeLandingPage(request.defaultLandingPage());

        pref.setLocale(request.locale());
        pref.setCurrencyCode(request.currencyCode().toUpperCase());
        pref.setReducedMotion(Boolean.TRUE.equals(request.reducedMotion()));
        pref.setDefaultLandingPage(normalizedLanding);
        pref.setTablePageSize(request.tablePageSize());
        pref.setOrderListPresetKey(normalizeOrderListPresetKey(request.orderListPresetKey()));
        pref.setOrderListStatusFilter(normalizeOrderListStatusFilter(request.orderListStatusFilter()));
        pref.setOrderListFulfillmentFilter(normalizeOrderListFulfillmentFilter(request.orderListFulfillmentFilter()));

        return UserPreferenceResponse.from(preferenceRepository.save(pref));
    }

    private UserPreference createDefault(AppUser user) {
        UserPreference pref = new UserPreference();
        pref.setUser(user);
        pref.setLocale("vi-VN");
        pref.setCurrencyCode("VND");
        pref.setReducedMotion(false);
        pref.setDefaultLandingPage("/orders");
        pref.setTablePageSize(15);
        pref.setOrderListPresetKey("ALL");
        pref.setOrderListStatusFilter("");
        pref.setOrderListFulfillmentFilter("ALL");
        return preferenceRepository.save(pref);
    }

    private AppUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String normalizeLandingPage(String value) {
        if (ALLOWED_LANDING_PAGES.contains(value)) {
            return value;
        }
        return "/orders";
    }

    private String normalizeOrderListPresetKey(String value) {
        if (ALLOWED_ORDER_LIST_PRESETS.contains(value)) {
            return value;
        }
        return "ALL";
    }

    private String normalizeOrderListStatusFilter(String value) {
        if (value == null) {
            return "";
        }
        if (ALLOWED_ORDER_STATUS_FILTERS.contains(value)) {
            return value;
        }
        return "";
    }

    private String normalizeOrderListFulfillmentFilter(String value) {
        if (ALLOWED_ORDER_FULFILLMENT_FILTERS.contains(value)) {
            return value;
        }
        return "ALL";
    }
}
