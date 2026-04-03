package com.sales.management.salesreturn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {
    boolean existsByOrderId(Long orderId);
    Optional<SalesReturn> findByOrderId(Long orderId);
}
