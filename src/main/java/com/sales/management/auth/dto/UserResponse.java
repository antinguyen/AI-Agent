package com.sales.management.auth.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String role,
        boolean active,
        Instant createdAt
) {}
