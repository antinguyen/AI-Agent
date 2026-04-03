package com.sales.management.product;

import com.sales.management.common.api.PageResponse;
import com.sales.management.product.dto.ProductCreateRequest;
import com.sales.management.product.dto.ProductImportResponse;
import com.sales.management.product.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ProductImageStorageService imageStorageService;

    public ProductController(ProductService productService, ProductImageStorageService imageStorageService) {
        this.productService = productService;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sku", required = false) String sku,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "originCountry", required = false) String originCountry,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "currencyCode", required = false) String currencyCode,
            @RequestParam(value = "priceFrom", required = false) java.math.BigDecimal priceFrom,
            @RequestParam(value = "priceTo", required = false) java.math.BigDecimal priceTo,
            @RequestParam(value = "yearFrom", required = false) Integer yearFrom,
            @RequestParam(value = "yearTo", required = false) Integer yearTo,
            @RequestParam(value = "active", required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return productService.list(name, sku, supplier, brand, originCountry, category, currencyCode,
                priceFrom, priceTo, yearFrom, yearTo, active, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable("id") Long id) {
        return productService.getById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable("id") Long id, @Valid @RequestBody ProductCreateRequest request) {
        return productService.update(id, request);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.listLowStock();
    }

    @GetMapping("/options")
    public ProductOptionsResponse options() {
        return productService.listOptions();
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadImageResponse uploadImage(@RequestParam("file") MultipartFile file) {
        return new UploadImageResponse(imageStorageService.store(file));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImportResponse importProducts(@RequestParam("file") MultipartFile file) {
        return productService.importCsv(file);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        productService.delete(id);
    }
}
