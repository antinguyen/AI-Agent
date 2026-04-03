package com.sales.management.report;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Native SQL queries returning Object[] rows for report aggregations.
 * Separate from JPA entity repositories to keep reporting concerns isolated.
 */
@Repository
public class ReportRepository {

    private final jakarta.persistence.EntityManager em;

    public ReportRepository(jakarta.persistence.EntityManager em) {
        this.em = em;
    }

    /**
     * Daily revenue from PAID orders in [fromInstant, toInstant).
     * Returns rows: [date_str VARCHAR, revenue DECIMAL, order_count BIGINT]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> dailyRevenue(Instant fromInstant, Instant toInstant) {
        String jpql = """
                SELECT CAST(p.paidAt AS LocalDate), SUM(COALESCE(p.vndAmount, p.amount)), COUNT(p.id)
                FROM Payment p
                WHERE p.paidAt >= :from AND p.paidAt < :to
                GROUP BY CAST(p.paidAt AS LocalDate)
                ORDER BY CAST(p.paidAt AS LocalDate)
                """;
        return em.createQuery(jpql)
                .setParameter("from", fromInstant)
                .setParameter("to", toInstant)
                .getResultList();
    }

    /**
     * Top-selling products by quantity in PAID/RETURNED orders.
     * Returns rows: [productId LONG, sku VARCHAR, name VARCHAR, totalQty LONG, totalRevenue DECIMAL]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> topProducts(int limit) {
        String jpql = """
                SELECT oi.product.id, oi.product.sku, oi.product.name,
                       SUM(oi.quantity), SUM(oi.lineTotal)
                FROM OrderItem oi
                JOIN oi.order o
                WHERE o.status IN (com.sales.management.order.OrderStatus.PAID,
                                   com.sales.management.order.OrderStatus.RETURNED)
                GROUP BY oi.product.id, oi.product.sku, oi.product.name
                ORDER BY SUM(oi.quantity) DESC
                """;
        return em.createQuery(jpql)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Order count grouped by status.
     * Returns rows: [status VARCHAR, count LONG]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> orderCountByStatus() {
        String jpql = """
                SELECT o.status, COUNT(o.id)
                FROM SalesOrder o
                GROUP BY o.status
                """;
        return em.createQuery(jpql).getResultList();
    }

    /**
     * Grand total revenue and total order count (all time, all PAID orders).
     * Returns single row: [totalRevenue DECIMAL, orderCount LONG]
     */
    public Object[] orderTotals() {
        String jpql = """
                SELECT SUM(o.totalAmount), COUNT(o.id)
                FROM SalesOrder o
                """;
        return (Object[]) em.createQuery(jpql).getSingleResult();
    }
}
