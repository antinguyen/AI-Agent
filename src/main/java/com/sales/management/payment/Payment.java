package com.sales.management.payment;

import com.sales.management.order.SalesOrder;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_payment_number", columnNames = "paymentNumber"),
        @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String paymentNumber;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private SalesOrder order;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** VND-equivalent of amount, computed from order.totalVnd at payment time. Null for legacy payments. */
    @Column(precision = 18, scale = 2)
    private BigDecimal vndAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod method;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant paidAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.paidAt = Instant.now();
        this.createdAt = this.paidAt;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public SalesOrder getOrder() {
        return order;
    }

    public void setOrder(SalesOrder order) {
        this.order = order;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getVndAmount() {
        return vndAmount;
    }

    public void setVndAmount(BigDecimal vndAmount) {
        this.vndAmount = vndAmount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
