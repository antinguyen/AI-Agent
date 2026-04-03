package com.sales.management.product.dto;

import java.util.List;

public record ProductImportResponse(
        int totalRows,
        int importedRows,
        int failedRows,
        List<String> errors
) {}
