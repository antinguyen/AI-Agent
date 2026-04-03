package com.sales.management.salesreturn;

import com.sales.management.order.SalesOrder;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_returns", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sales_returns_return_number", columnNames = "returnNumber"),
        @UniqueConstraint(name = "uk_sales_returns_order_id", columnNames = "order_id")
})
public class SalesReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String returnNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder order;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SalesReturnStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRefund;

    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesReturnItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(SalesReturnItem item) {
        item.setSalesReturn(this);
        this.items.add(item);
    }

    public Long getId() { return id; }

    public String getReturnNumber() { return returnNumber; }
    public void setReturnNumber(String returnNumber) { this.returnNumber = returnNumber; }

    public SalesOrder getOrder() { return order; }
    public void setOrder(SalesOrder order) { this.order = order; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public SalesReturnStatus getStatus() { return status; }
    public void setStatus(SalesReturnStatus status) { this.status = status; }

    public BigDecimal getTotalRefund() { return totalRefund; }
    public void setTotalRefund(BigDecimal totalRefund) { this.totalRefund = totalRefund; }

    public List<SalesReturnItem> getItems() { return items; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
