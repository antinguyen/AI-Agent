package com.sales.management.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64)
        String username,

        @NotBlank @Size(min = 8, max = 128)
        String password,

        @NotBlank @Pattern(regexp = "ADMIN|STAFF", message = "role must be ADMIN or STAFF")
        String role
) {}
