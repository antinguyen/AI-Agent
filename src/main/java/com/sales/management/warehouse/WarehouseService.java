package com.sales.management.warehouse;

import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.warehouse.dto.ProductStockByWarehouseResponse;
import com.sales.management.warehouse.dto.ProductStockByWarehouseResponse.WarehouseStockDetail;
import com.sales.management.warehouse.dto.WarehouseCreateRequest;
import com.sales.management.warehouse.dto.WarehouseResponse;
import com.sales.management.warehouse.dto.WarehouseStockItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@SuppressWarnings("null")
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ProductWarehouseStockRepository stockRepository;
    private final StockTransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    public WarehouseService(WarehouseRepository warehouseRepository, 
                           ProductWarehouseStockRepository stockRepository,
                           StockTransactionRepository transactionRepository,
                           ProductRepository productRepository) {
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public WarehouseResponse create(WarehouseCreateRequest request) {
        if (warehouseRepository.existsByCode(request.code)) {
            throw new DuplicateResourceException("Warehouse code already exists: " + request.code);
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.code.trim());
        warehouse.setName(request.name.trim());
        warehouse.setAddress(request.address != null ? request.address.trim() : null);
        warehouse.setActive(request.active != null ? request.active : true);

        Warehouse saved = warehouseRepository.save(warehouse);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
        return toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> listActive() {
        return warehouseRepository.findByActiveTrueOrderByName().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> listAll() {
        return warehouseRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseCreateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));

        if (!warehouse.getCode().equals(request.code) && warehouseRepository.existsByCode(request.code)) {
            throw new DuplicateResourceException("Warehouse code already exists: " + request.code);
        }

        warehouse.setCode(request.code.trim());
        warehouse.setName(request.name.trim());
        warehouse.setAddress(request.address != null ? request.address.trim() : null);
        warehouse.setActive(request.active != null ? request.active : true);

        Warehouse saved = warehouseRepository.save(warehouse);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Warehouse not found: " + id);
        }
        warehouseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ProductStockByWarehouseResponse getProductStockByWarehouses(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        List<ProductWarehouseStock> stocks = stockRepository.findByProductId(productId);

        List<WarehouseStockDetail> details = stocks.stream()
                .map(pws -> new WarehouseStockDetail(
                        pws.getWarehouse().getId(),
                        pws.getWarehouse().getName(),
                        pws.getQuantity(),
                        pws.getReservedQuantity(),
                        pws.getAvailableQuantity(),
                        pws.getLowStockThreshold(),
                        pws.getQuantity() <= pws.getLowStockThreshold()
                ))
                .toList();

        int totalQty = stocks.stream().mapToInt(ProductWarehouseStock::getQuantity).sum();
        int totalReserved = stocks.stream().mapToInt(ProductWarehouseStock::getReservedQuantity).sum();
        int totalAvailable = stocks.stream().mapToInt(ProductWarehouseStock::getAvailableQuantity).sum();

        return new ProductStockByWarehouseResponse(
                productId, product.getSku(), product.getName(), details,
                totalQty, totalReserved, totalAvailable
        );
    }

    @Transactional
    public void reserveStock(Long productId, Long warehouseId, Integer quantity, String referenceType, Long referenceId) {
        ProductWarehouseStock stock = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found for product + warehouse"));

        stock.reserve(quantity);
        stockRepository.save(stock);

        // Record transaction
        StockTransaction transaction = new StockTransaction(
                productId, warehouseId, StockTransaction.TransactionType.RESERVATION,
                -quantity, referenceType, referenceId, "Reserved for order"
        );
        transactionRepository.save(transaction);
    }

    @Transactional
    public void releaseStock(Long productId, Long warehouseId, Integer quantity) {
        ProductWarehouseStock stock = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        stock.release(quantity);
        stockRepository.save(stock);

        // Record transaction
        StockTransaction transaction = new StockTransaction(
                productId, warehouseId, StockTransaction.TransactionType.RELEASE,
                quantity, null, null, "Released reservation"
        );
        transactionRepository.save(transaction);
    }

    @Transactional
    public void deductStock(Long productId, Long warehouseId, Integer quantity, String referenceType, Long referenceId) {
        ProductWarehouseStock stock = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        stock.deduct(quantity);
        stockRepository.save(stock);

        // Record transaction
        StockTransaction transaction = new StockTransaction(
                productId, warehouseId, StockTransaction.TransactionType.SALES,
                -quantity, referenceType, referenceId, "Deducted for shipped order"
        );
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<WarehouseStockItemResponse> getStockByWarehouse(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }
        return stockRepository.findByWarehouseIdWithProduct(warehouseId).stream()
                .map(pws -> new WarehouseStockItemResponse(
                        pws.getProduct().getId(),
                        pws.getProduct().getSku(),
                        pws.getProduct().getName(),
                        pws.getQuantity(),
                        pws.getReservedQuantity(),
                        pws.getAvailableQuantity(),
                        pws.getLowStockThreshold(),
                        pws.getQuantity() <= pws.getLowStockThreshold()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Warehouse getDefaultWarehouse() {
        return warehouseRepository.findByCode("WH-DEFAULT")
                .orElseThrow(() -> new ResourceNotFoundException("Default warehouse not found"));
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouseEntity(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    }

    @Transactional(readOnly = true)
    public Warehouse getDefaultWarehouseOrNull() {
        return warehouseRepository.findByCode("WH-DEFAULT").orElse(null);
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getActive(),
                warehouse.getCreatedAt().toString(),
                warehouse.getCreatedBy()
        );
    }
}
