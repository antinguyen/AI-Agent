package com.sales.management.salesreturn.dto;

import java.math.BigDecimal;

public record SalesReturnItemResponse(
        Long orderItemId,
        Long productId,
        String productSku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
