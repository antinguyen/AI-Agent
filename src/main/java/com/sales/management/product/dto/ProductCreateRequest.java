package com.sales.management.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductCreateRequest {

    @NotBlank(message = "sku is required")
    @Size(max = 64, message = "sku max length is 64")
    private String sku;

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name max length is 255")
    private String name;

    @Size(max = 2000, message = "description max length is 2000")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "purchasePrice must be greater than 0")
    private BigDecimal purchasePrice;

    @Size(max = 32, message = "unit max length is 32")
    private String unit;

    @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be ISO-4217 format (e.g. VND, USD)")
    private String currencyCode;

    @DecimalMin(value = "0.000001", inclusive = true, message = "exchangeRate must be greater than 0")
    private BigDecimal exchangeRate;

    @Size(max = 512, message = "imageUrl max length is 512")
    private String imageUrl;

    @Size(max = 255, message = "supplier max length is 255")
    private String supplier;

    @Size(max = 255, message = "brand max length is 255")
    private String brand;

    @Size(max = 128, message = "originCountry max length is 128")
    private String originCountry;

    @Size(max = 100, message = "category max length is 100")
    private String category;

    @DecimalMin(value = "0.0", inclusive = true, message = "vatRate must be >= 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "vatRate must be <= 100")
    private BigDecimal vatRate;

    @Min(value = 1900, message = "manufactureYear must be >= 1900")
    @Max(value = 2100, message = "manufactureYear must be <= 2100")
    private Integer manufactureYear;

    @NotNull(message = "stockQuantity is required")
    @PositiveOrZero(message = "stockQuantity must be >= 0")
    private Integer stockQuantity;

    @NotNull(message = "active is required")
    private Boolean active;

    @Min(value = 0, message = "lowStockThreshold must be >= 0")
    private Integer lowStockThreshold;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public Integer getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(Integer manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }
}
