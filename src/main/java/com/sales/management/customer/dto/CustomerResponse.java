package com.sales.management.customer.dto;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String code,
        String name,
        String phone,
        String email,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
