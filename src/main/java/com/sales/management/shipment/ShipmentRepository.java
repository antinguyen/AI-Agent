package com.sales.management.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);
    List<Shipment> findAllByOrderIdIn(Collection<Long> orderIds);
    boolean existsByOrderId(Long orderId);
}
