package com.sales.management.product;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "currency_exchange_rates")
public class CurrencyExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3, unique = true)
    private String currencyCode;

    @Column(nullable = false, length = 128)
    private String bankName;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal rateToVnd;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getRateToVnd() {
        return rateToVnd;
    }

    public void setRateToVnd(BigDecimal rateToVnd) {
        this.rateToVnd = rateToVnd;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
