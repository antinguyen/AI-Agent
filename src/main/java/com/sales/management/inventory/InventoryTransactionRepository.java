package com.sales.management.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Page<InventoryTransaction> findByProductId(Long productId, Pageable pageable);

    Page<InventoryTransaction> findByOrderId(Long orderId, Pageable pageable);
}
