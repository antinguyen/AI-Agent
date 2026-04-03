package com.sales.management.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        String username,
        String role
) {}
