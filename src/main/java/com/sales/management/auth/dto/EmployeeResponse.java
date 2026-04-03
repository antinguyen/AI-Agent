package com.sales.management.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sales.management.auth.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * DTO for employee response data
 * Contains all employee information except password
 */
public class EmployeeResponse {

    private Long id;
    private String username;
    private String role;
    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private ZonedDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private ZonedDateTime updatedAt;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private String position;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hiringDate;

    private BigDecimal salary;

    // Constructors
    public EmployeeResponse() {
    }

    public EmployeeResponse(Employee employee) {
        this.id = employee.getId();
        this.username = employee.getUsername();
        this.role = employee.getRole().toString();
        this.active = employee.getActive();
        this.createdAt = employee.getCreatedAt();
        this.updatedAt = employee.getUpdatedAt();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
        this.email = employee.getEmail();
        this.phone = employee.getPhone();
        this.department = employee.getDepartment();
        this.position = employee.getPosition();
        this.hiringDate = employee.getHiringDate();
        this.salary = employee.getSalary();
    }

    /**
     * Create EmployeeResponse from Employee without salary (for public profiles)
     */
    public static EmployeeResponse publicView(Employee employee) {
        EmployeeResponse response = new EmployeeResponse(employee);
        response.salary = null;  // Don't expose salary in public views
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
