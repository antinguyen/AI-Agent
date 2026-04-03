package com.sales.management.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductWarehouseStockRepository extends JpaRepository<ProductWarehouseStock, ProductWarehouseStockId> {
    Optional<ProductWarehouseStock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    List<ProductWarehouseStock> findByProductId(Long productId);
    List<ProductWarehouseStock> findByWarehouseId(Long warehouseId);
    void deleteByProductId(Long productId);

        @Query("SELECT pws FROM ProductWarehouseStock pws JOIN FETCH pws.product WHERE pws.warehouse.id = :warehouseId ORDER BY pws.product.name")
        List<ProductWarehouseStock> findByWarehouseIdWithProduct(@Param("warehouseId") Long warehouseId);
    
    @Query("SELECT pws FROM ProductWarehouseStock pws " +
           "WHERE pws.warehouse.id = :warehouseId AND pws.quantity <= pws.lowStockThreshold")
    List<ProductWarehouseStock> findLowStockByWarehouse(@Param("warehouseId") Long warehouseId);
}
