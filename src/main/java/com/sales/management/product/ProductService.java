package com.sales.management.product;

import com.sales.management.common.api.PageResponse;
import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.product.dto.ProductCreateRequest;
import com.sales.management.product.dto.ProductImportResponse;
import com.sales.management.product.dto.ProductResponse;
import com.sales.management.warehouse.ProductWarehouseStockRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@SuppressWarnings("null")
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrencyRateService currencyRateService;
    private final ProductWarehouseStockRepository productWarehouseStockRepository;

    public ProductService(ProductRepository productRepository,
                          CurrencyRateService currencyRateService,
                          ProductWarehouseStockRepository productWarehouseStockRepository) {
        this.productRepository = productRepository;
        this.currencyRateService = currencyRateService;
        this.productWarehouseStockRepository = productWarehouseStockRepository;
    }

    @Transactional
    @CacheEvict(value = {"products", "products-low-stock"}, allEntries = true)
    public ProductResponse create(ProductCreateRequest request) {
        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new DuplicateResourceException("Product SKU already exists: " + request.getSku());
        }

        Product product = new Product();
        applyRequest(product, request);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"products", "products-low-stock"}, allEntries = true)
    public ProductImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File import is empty");
        }

        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int importedRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new BusinessRuleException("CSV header is missing");
            }

            Map<String, Integer> headerIndex = buildHeaderIndex(parseCsvLine(headerLine));
            requireHeader(headerIndex, "sku");
            requireHeader(headerIndex, "name");
            requireHeader(headerIndex, "price");
            requireHeader(headerIndex, "stockquantity");

            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;

                try {
                    List<String> cols = parseCsvLine(line);
                    ProductCreateRequest request = toCreateRequest(cols, headerIndex);

                    if (productRepository.findBySku(request.getSku()).isPresent()) {
                        throw new DuplicateResourceException("Product SKU already exists: " + request.getSku());
                    }

                    Product product = new Product();
                    applyRequest(product, request);
                    productRepository.save(product);
                    importedRows++;
                } catch (Exception ex) {
                    errors.add("Line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BusinessRuleException("Cannot read CSV file");
        }

        return new ProductImportResponse(totalRows, importedRows, totalRows - importedRows, errors);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "products",
            key = "T(java.util.Objects).toString(#name, '')+':' + T(java.util.Objects).toString(#sku, '')+':' + T(java.util.Objects).toString(#supplier, '')+':' + T(java.util.Objects).toString(#brand, '')+':' + T(java.util.Objects).toString(#originCountry, '')+':' + T(java.util.Objects).toString(#category, '')+':' + T(java.util.Objects).toString(#currencyCode, '')+':' + T(java.util.Objects).toString(#priceFrom, '')+':' + T(java.util.Objects).toString(#priceTo, '')+':' + T(java.util.Objects).toString(#yearFrom, '')+':' + T(java.util.Objects).toString(#yearTo, '')+':' + T(java.util.Objects).toString(#active, '')+':' + (#pageable == null ? 'np' : #pageable.pageNumber)+':' + (#pageable == null ? 'ns' : #pageable.pageSize)+':' + (#pageable == null ? 'nosort' : #pageable.sort.toString())")
    public PageResponse<ProductResponse> list(String name, String sku, String supplier, String brand,
                      String originCountry, String category, String currencyCode,
                              BigDecimal priceFrom, BigDecimal priceTo,
                              Integer yearFrom, Integer yearTo,
                                              Boolean active, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (sku != null && !sku.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("sku")), "%" + sku.toLowerCase() + "%"));
            }
            if (supplier != null && !supplier.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("supplier")), "%" + supplier.toLowerCase() + "%"));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%"));
            }
            if (originCountry != null && !originCountry.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("originCountry")), "%" + originCountry.toLowerCase() + "%"));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("category")), "%" + category.toLowerCase() + "%"));
            }
            if (currencyCode != null && !currencyCode.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("currencyCode")), currencyCode.trim().toUpperCase()));
            }
            if (priceFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceFrom));
            }
            if (priceTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceTo));
            }
            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("manufactureYear"), yearFrom));
            }
            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("manufactureYear"), yearTo));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(productRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Long productId = Objects.requireNonNull(id, "id is required");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return toResponse(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "products-low-stock"}, allEntries = true)
    public ProductResponse update(Long id, ProductCreateRequest request) {
        Long productId = Objects.requireNonNull(id, "id is required");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (productRepository.existsBySkuAndIdNot(request.getSku(), productId)) {
            throw new DuplicateResourceException("Product SKU already exists: " + request.getSku());
        }

        applyRequest(product, request);
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"products", "products-low-stock"}, allEntries = true)
    public void delete(Long id) {
        Long productId = Objects.requireNonNull(id, "id is required");
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productWarehouseStockRepository.deleteByProductId(productId);
        productRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products-low-stock")
    public List<ProductResponse> listLowStock() {
        return productRepository.findLowStockProducts().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductOptionsResponse listOptions() {
        return new ProductOptionsResponse(
                productRepository.findDistinctSuppliers(),
                productRepository.findDistinctBrands(),
                productRepository.findDistinctOriginCountries(),
                productRepository.findDistinctCategories(),
                currencyRateService.listRates()
        );
    }

    private void applyRequest(Product product, ProductCreateRequest request) {
        String normalizedCurrency = normalizeOrDefault(request.getCurrencyCode(), "VND").toUpperCase();
        product.setSku(request.getSku().trim());
        product.setName(request.getName().trim());
        product.setDescription(normalizeNullable(request.getDescription()));
        product.setPrice(request.getPrice());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setUnit(normalizeOrDefault(request.getUnit(), "pcs"));
        product.setCurrencyCode(normalizedCurrency);
        product.setExchangeRate(currencyRateService.resolveRateToVnd(normalizedCurrency));
        product.setImageUrl(normalizeNullable(request.getImageUrl()));
        product.setSupplier(normalizeNullable(request.getSupplier()));
        product.setBrand(normalizeNullable(request.getBrand()));
        product.setOriginCountry(normalizeNullable(request.getOriginCountry()));
        product.setCategory(normalizeOrDefault(request.getCategory(), "General"));
        product.setVatRate(request.getVatRate() != null ? request.getVatRate() : BigDecimal.ZERO);
        product.setManufactureYear(request.getManufactureYear());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive());
        product.setLowStockThreshold(
                request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getPurchasePrice(),
                product.getUnit(),
                product.getCurrencyCode(),
                product.getExchangeRate(),
                product.getImageUrl(),
                product.getSupplier(),
                product.getBrand(),
                product.getOriginCountry(),
                product.getCategory(),
                product.getVatRate(),
                product.getManufactureYear(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCreatedBy(),
                product.getUpdatedBy()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalizeNullable(value);
        return normalized != null ? normalized : defaultValue;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        return map;
    }

    private void requireHeader(Map<String, Integer> headerIndex, String header) {
        if (!headerIndex.containsKey(header)) {
            throw new BusinessRuleException("Missing required CSV header: " + header);
        }
    }

    private ProductCreateRequest toCreateRequest(List<String> cols, Map<String, Integer> headerIndex) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku(getRequired(cols, headerIndex, "sku"));
        request.setName(getRequired(cols, headerIndex, "name"));
        request.setPrice(parseBigDecimal(getRequired(cols, headerIndex, "price"), "price"));
        request.setStockQuantity(parseInteger(getRequired(cols, headerIndex, "stockquantity"), "stockQuantity"));

        request.setDescription(getOptional(cols, headerIndex, "description"));
        request.setPurchasePrice(parseOptionalBigDecimal(getOptional(cols, headerIndex, "purchaseprice"), "purchasePrice"));
        request.setUnit(getOptional(cols, headerIndex, "unit"));
        request.setCurrencyCode(getOptional(cols, headerIndex, "currencycode"));
        request.setImageUrl(getOptional(cols, headerIndex, "imageurl"));
        request.setSupplier(getOptional(cols, headerIndex, "supplier"));
        request.setBrand(getOptional(cols, headerIndex, "brand"));
        request.setOriginCountry(getOptional(cols, headerIndex, "origincountry"));
        request.setManufactureYear(parseOptionalInteger(getOptional(cols, headerIndex, "manufactureyear"), "manufactureYear"));
        request.setLowStockThreshold(parseOptionalInteger(getOptional(cols, headerIndex, "lowstockthreshold"), "lowStockThreshold"));

        String activeRaw = getOptional(cols, headerIndex, "active");
        request.setActive(activeRaw == null || activeRaw.isBlank() || Boolean.parseBoolean(activeRaw.trim()));
        return request;
    }

    private String getRequired(List<String> cols, Map<String, Integer> headerIndex, String key) {
        String value = getOptional(cols, headerIndex, key);
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Missing required value: " + key);
        }
        return value.trim();
    }

    private String getOptional(List<String> cols, Map<String, Integer> headerIndex, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx < 0 || idx >= cols.size()) {
            return null;
        }
        String value = cols.get(idx);
        return value == null ? null : value.trim();
    }

    private BigDecimal parseBigDecimal(String value, String field) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("Invalid decimal for " + field + ": " + value);
        }
    }

    private BigDecimal parseOptionalBigDecimal(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseBigDecimal(value, field);
    }

    private Integer parseInteger(String value, String field) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("Invalid number for " + field + ": " + value);
        }
    }

    private Integer parseOptionalInteger(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseInteger(value, field);
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        cells.add(current.toString());

        return Arrays.asList(cells.toArray(new String[0]));
    }
}
