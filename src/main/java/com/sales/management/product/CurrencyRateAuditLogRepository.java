package com.sales.management.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRateAuditLogRepository extends JpaRepository<CurrencyRateAuditLog, Long> {

    Page<CurrencyRateAuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);
}
