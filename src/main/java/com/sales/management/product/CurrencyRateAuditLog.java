package com.sales.management.product;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "currency_rate_audit_log")
public class CurrencyRateAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(length = 128)
    private String oldBankName;

    @Column(nullable = false, length = 128)
    private String newBankName;

    @Column(precision = 18, scale = 6)
    private BigDecimal oldRate;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal newRate;

    /** UPSERT or RESET */
    @Column(nullable = false, length = 16)
    private String action;

    @Column(length = 64)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        this.changedAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getOldBankName() { return oldBankName; }
    public void setOldBankName(String oldBankName) { this.oldBankName = oldBankName; }

    public String getNewBankName() { return newBankName; }
    public void setNewBankName(String newBankName) { this.newBankName = newBankName; }

    public BigDecimal getOldRate() { return oldRate; }
    public void setOldRate(BigDecimal oldRate) { this.oldRate = oldRate; }

    public BigDecimal getNewRate() { return newRate; }
    public void setNewRate(BigDecimal newRate) { this.newRate = newRate; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getChangedAt() { return changedAt; }
}
