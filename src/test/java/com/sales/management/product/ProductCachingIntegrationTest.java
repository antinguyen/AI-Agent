package com.sales.management.product;

import com.sales.management.common.api.PageResponse;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.product.dto.ProductCreateRequest;
import com.sales.management.product.dto.ProductResponse;
import com.sales.management.salesreturn.SalesReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@SuppressWarnings({"unchecked", "null"})
class ProductCachingIntegrationTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;

    @MockitoSpyBean
    private ProductRepository spyProductRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
        reset(spyProductRepository);
    }

    @Test
    void shouldCacheProductListResults() {
        productService.create(newRequest("SKU-CACHE-01", "Cache Product 1"));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        PageResponse<ProductResponse> first = productService.list(null, null, null, null, null, null,
            null, null, null, null, null, true, pageable);
        PageResponse<ProductResponse> second = productService.list(null, null, null, null, null, null,
            null, null, null, null, null, true, pageable);

        assertThat(first.totalElements()).isEqualTo(1);
        assertThat(second.totalElements()).isEqualTo(1);
        verify(spyProductRepository, times(1)).findAll(
            (Specification<Product>) any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldEvictCacheAfterCreate() {
        productService.create(newRequest("SKU-CACHE-02", "Cache Product 2"));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        productService.list(null, null, null, null, null, null,
            null, null, null, null, null, true, pageable);
        verify(spyProductRepository, times(1)).findAll(
            (Specification<Product>) any(Specification.class), any(Pageable.class));

        productService.create(newRequest("SKU-CACHE-03", "Cache Product 3"));
        productService.list(null, null, null, null, null, null,
            null, null, null, null, null, true, pageable);

        verify(spyProductRepository, times(2)).findAll(
            (Specification<Product>) any(Specification.class), any(Pageable.class));
    }

    private ProductCreateRequest newRequest(String sku, String name) {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setSku(sku);
        req.setName(name);
        req.setPrice(new BigDecimal("99.99"));
        req.setStockQuantity(100);
        req.setLowStockThreshold(10);
        req.setActive(true);
        return req;
    }
}


