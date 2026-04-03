package com.sales.management.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for updating employee role
 */
public class UpdateEmployeeRoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|HR|MANAGER|EMPLOYEE", message = "Role must be ADMIN, HR, MANAGER, or EMPLOYEE")
    private String role;

    // Constructors
    public UpdateEmployeeRoleRequest() {
    }

    public UpdateEmployeeRoleRequest(String role) {
        this.role = role;
    }

    // Getters and Setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
