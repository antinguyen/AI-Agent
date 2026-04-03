package com.sales.management.auth.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO for updating employee profile information
 */
public class UpdateEmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$|^$",
        message = "Phone number must be valid (E.164 format)"
    )
    private String phone;

    @NotBlank(message = "Department is required")
    @Size(min = 1, max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @NotBlank(message = "Position is required")
    @Size(min = 1, max = 100, message = "Position must not exceed 100 characters")
    private String position;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be greater than 0")
    private BigDecimal salary;

    // Constructors
    public UpdateEmployeeRequest() {
    }

    public UpdateEmployeeRequest(String firstName, String lastName, String email,
                                String phone, String department, String position, BigDecimal salary) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.position = position;
        this.salary = salary;
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }
}
