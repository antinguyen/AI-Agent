package com.sales.management.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeRoleRequest(
        @NotBlank
        @Pattern(regexp = "ADMIN|STAFF", message = "Role must be ADMIN or STAFF")
        String role
) {}
