package com.sales.management.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.lowStockThreshold AND p.active = true")
    List<Product> findLowStockProducts();

    @Query("SELECT DISTINCT p.supplier FROM Product p WHERE p.supplier IS NOT NULL AND p.supplier <> '' ORDER BY p.supplier")
    List<String> findDistinctSuppliers();

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand <> '' ORDER BY p.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT p.originCountry FROM Product p WHERE p.originCountry IS NOT NULL AND p.originCountry <> '' ORDER BY p.originCountry")
    List<String> findDistinctOriginCountries();

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.category <> '' ORDER BY p.category")
    List<String> findDistinctCategories();
}
