package com.sales.management.preference;

import com.sales.management.auth.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false)
    private boolean reducedMotion;

    @Column(nullable = false, length = 64)
    private String defaultLandingPage;

    @Column(nullable = false)
    private int tablePageSize;

    @Column(nullable = false, length = 32)
    private String orderListPresetKey;

    @Column(nullable = false, length = 16)
    private String orderListStatusFilter;

    @Column(nullable = false, length = 32)
    private String orderListFulfillmentFilter;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isReducedMotion() {
        return reducedMotion;
    }

    public void setReducedMotion(boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
    }

    public String getDefaultLandingPage() {
        return defaultLandingPage;
    }

    public void setDefaultLandingPage(String defaultLandingPage) {
        this.defaultLandingPage = defaultLandingPage;
    }

    public int getTablePageSize() {
        return tablePageSize;
    }

    public void setTablePageSize(int tablePageSize) {
        this.tablePageSize = tablePageSize;
    }

    public String getOrderListPresetKey() {
        return orderListPresetKey;
    }

    public void setOrderListPresetKey(String orderListPresetKey) {
        this.orderListPresetKey = orderListPresetKey;
    }

    public String getOrderListStatusFilter() {
        return orderListStatusFilter;
    }

    public void setOrderListStatusFilter(String orderListStatusFilter) {
        this.orderListStatusFilter = orderListStatusFilter;
    }

    public String getOrderListFulfillmentFilter() {
        return orderListFulfillmentFilter;
    }

    public void setOrderListFulfillmentFilter(String orderListFulfillmentFilter) {
        this.orderListFulfillmentFilter = orderListFulfillmentFilter;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
