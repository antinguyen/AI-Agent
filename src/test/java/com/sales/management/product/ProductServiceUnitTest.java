package com.sales.management.product;

import com.sales.management.common.api.PageResponse;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.product.dto.ProductCreateRequest;
import com.sales.management.product.dto.ProductResponse;
import com.sales.management.warehouse.ProductWarehouseStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CurrencyRateService currencyRateService;

    @Mock
    private ProductWarehouseStockRepository productWarehouseStockRepository;

    @InjectMocks
    private ProductService productService;

    private Product savedProduct;
    private ProductCreateRequest request;

    @BeforeEach
    void setUp() {
        savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setSku("SKU-001");
        savedProduct.setName("Test Product");
        savedProduct.setPrice(new BigDecimal("150000"));
        savedProduct.setStockQuantity(10);
        savedProduct.setActive(true);

        request = new ProductCreateRequest();
        request.setSku("SKU-001");
        request.setName("Test Product");
        request.setPrice(new BigDecimal("150000"));
        request.setStockQuantity(10);
        request.setActive(true);

        lenient().when(currencyRateService.resolveRateToVnd(any())).thenReturn(BigDecimal.ONE);
    }

    // --- create() ---

    @Test
    void create_shouldReturnResponse_whenSkuIsUnique() {
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.create(request);

        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.name()).isEqualTo("Test Product");
        assertThat(response.price()).isEqualByComparingTo("150000");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_shouldTrimSku_andName() {
        request.setSku("  SKU-001  ");
        request.setName("  Test Product  ");

        // ProductService calls findBySku(request.getSku()) before trimming, so stub with untrimmed value
        when(productRepository.findBySku("  SKU-001  ")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.name()).isEqualTo("Test Product");
    }

    @Test
    void create_shouldThrowDuplicateException_whenSkuAlreadyExists() {
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(savedProduct));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-001");

        verify(productRepository, never()).save(any());
    }

    // --- list() ---

    @Test
    void list_shouldReturnAllProducts() {
        Product second = new Product();
        second.setId(2L);
        second.setSku("SKU-002");
        second.setName("Second Product");
        second.setPrice(new BigDecimal("200000"));
        second.setStockQuantity(5);
        second.setActive(false);

        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedProduct, second), pageable, 2));

        PageResponse<ProductResponse> result = productService.list(null, null, null, null, null, null,
            null, null, null, null, null, null, pageable);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.content().get(1).sku()).isEqualTo("SKU-002");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void list_shouldReturnEmptyList_whenNoProducts() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ProductResponse> result = productService.list(null, null, null, null, null, null,
            null, null, null, null, null, null, pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // --- getById() ---

    @Test
    void getById_shouldReturnResponse_whenProductExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));

        ProductResponse response = productService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-001");
    }

    @Test
    void getById_shouldThrowNotFoundException_whenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- update() ---

    @Test
    void update_shouldReturnUpdatedResponse_whenValid() {
        ProductCreateRequest updateRequest = new ProductCreateRequest();
        updateRequest.setSku("SKU-001-UPD");
        updateRequest.setName("Updated Product");
        updateRequest.setPrice(new BigDecimal("999000"));
        updateRequest.setStockQuantity(99);
        updateRequest.setActive(false);

        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productRepository.existsBySkuAndIdNot("SKU-001-UPD", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.update(1L, updateRequest);

        assertThat(response.sku()).isEqualTo("SKU-001-UPD");
        assertThat(response.name()).isEqualTo("Updated Product");
        assertThat(response.stockQuantity()).isEqualTo(99);
        assertThat(response.active()).isFalse();
    }

    @Test
    void update_shouldThrowNotFoundException_whenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_shouldThrowDuplicateException_whenSkuConflictsWithAnotherProduct() {
        ProductCreateRequest updateRequest = new ProductCreateRequest();
        updateRequest.setSku("SKU-EXISTING");
        updateRequest.setName("Updated");
        updateRequest.setPrice(new BigDecimal("100000"));
        updateRequest.setStockQuantity(1);
        updateRequest.setActive(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productRepository.existsBySkuAndIdNot("SKU-EXISTING", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-EXISTING");

        verify(productRepository, never()).save(any());
    }

    // --- delete() ---

    @Test
    void delete_shouldDeleteSuccessfully_whenProductExists() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productWarehouseStockRepository).deleteByProductId(1L);
        doNothing().when(productRepository).deleteById(1L);

        productService.delete(1L);

        verify(productWarehouseStockRepository).deleteByProductId(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenProductDoesNotExist() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).deleteById(any());
    }
}


