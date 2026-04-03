package com.sales.management.auth.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new employee
 */
public class CreateEmployeeRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|HR|MANAGER|EMPLOYEE", message = "Role must be ADMIN, HR, MANAGER, or EMPLOYEE")
    private String role;

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

    @NotNull(message = "Hiring date is required")
    @PastOrPresent(message = "Hiring date must not be in the future")
    private LocalDate hiringDate;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be greater than 0")
    private BigDecimal salary;

    // Constructors
    public CreateEmployeeRequest() {
    }

    public CreateEmployeeRequest(String username, String password, String role,
                                 String firstName, String lastName, String email, String phone,
                                 String department, String position, LocalDate hiringDate, BigDecimal salary) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.position = position;
        this.hiringDate = hiringDate;
        this.salary = salary;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

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

    public LocalDate getHiringDate() {
        return hiringDate;
    }

    public void setHiringDate(LocalDate hiringDate) {
        this.hiringDate = hiringDate;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }
}
