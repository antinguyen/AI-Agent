package com.sales.management.warehouse;

import com.sales.management.warehouse.dto.ProductStockByWarehouseResponse;
import com.sales.management.warehouse.dto.WarehouseCreateRequest;
import com.sales.management.warehouse.dto.WarehouseResponse;
import com.sales.management.warehouse.dto.WarehouseStockItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody WarehouseCreateRequest request) {
        return warehouseService.create(request);
    }

    @GetMapping
    public List<WarehouseResponse> list() {
        return warehouseService.listActive();
    }

    @GetMapping("/all")
    public List<WarehouseResponse> listAll() {
        return warehouseService.listAll();
    }

    @GetMapping("/{id}")
    public WarehouseResponse getById(@PathVariable("id") Long id) {
        return warehouseService.getById(id);
    }

    @PutMapping("/{id}")
    public WarehouseResponse update(@PathVariable("id") Long id, @Valid @RequestBody WarehouseCreateRequest request) {
        return warehouseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        warehouseService.delete(id);
    }

    @GetMapping("/products/{productId}/stock")
    public ProductStockByWarehouseResponse getProductStock(@PathVariable("productId") Long productId) {
        return warehouseService.getProductStockByWarehouses(productId);
    }

    @GetMapping("/{id}/stock")
    public List<WarehouseStockItemResponse> getWarehouseStock(@PathVariable("id") Long id) {
        return warehouseService.getStockByWarehouse(id);
    }
}
