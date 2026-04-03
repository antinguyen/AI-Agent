package com.sales.management.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.createdAt >= :from")
    long countCreatedSince(@Param("from") Instant from);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM SalesOrder o WHERE o.status = :status AND o.createdAt >= :from")
    BigDecimal sumTotalAmountByStatusSince(@Param("status") OrderStatus status, @Param("from") Instant from);
}
