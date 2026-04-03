package com.sales.management.auth;

import com.sales.management.auth.dto.CreateEmployeeRequest;
import com.sales.management.auth.dto.EmployeeResponse;
import com.sales.management.auth.dto.UpdateEmployeeRequest;
import com.sales.management.auth.dto.UpdateEmployeeRoleRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing employees
 * Handles CRUD operations and employee-specific business logic
 */
@Service
public class EmployeeManagementService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeManagementService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeManagementService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new employee
     */
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee: {}", request.getUsername());

        // Check if username exists
        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check if email exists
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create new employee
        Employee employee = new Employee();
        employee.setUsername(request.getUsername());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setRole(EmployeeRole.valueOf(request.getRole()));
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setHiringDate(request.getHiringDate());
        employee.setSalary(request.getSalary());
        employee.setActive(true);

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created successfully: id={}, username={}", saved.getId(), saved.getUsername());

        return new EmployeeResponse(saved);
    }

    /**
     * Get employee by ID
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        log.debug("Fetching employee: id={}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));
        return new EmployeeResponse(employee);
    }

    /**
     * List employees with pagination and filters
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listEmployees(
        String username, String firstName, String lastName, String email,
        String department, String position, String role, Boolean active,
        Pageable pageable) {

        log.debug("Listing employees with filters: username={}, dept={}, role={}", username, department, role);

        EmployeeRole roleEnum = role != null ? EmployeeRole.valueOf(role) : null;

        Page<Employee> employees = employeeRepository.findEmployees(
            username, firstName, lastName, email,
            department, position, roleEnum, active, pageable
        );

        List<EmployeeResponse> content = employees.getContent().stream()
            .map(EmployeeResponse::new)
            .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, employees.getTotalElements());
    }

    /**
     * Update employee profile
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee profile: id={}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        // Check email uniqueness
        if (!employee.getEmail().equals(request.getEmail())) {
            if (employeeRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
        }

        // Update fields
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setSalary(request.getSalary());

        Employee updated = employeeRepository.save(employee);
        log.info("Employee profile updated: id={}", id);

        return new EmployeeResponse(updated);
    }

    /**
     * Change employee role
     */
    @Transactional
    public EmployeeResponse changeRole(Long id, UpdateEmployeeRoleRequest request) {
        log.info("Changing employee role: id={}, newRole={}", id, request.getRole());

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        EmployeeRole oldRole = employee.getRole();
        EmployeeRole newRole = EmployeeRole.valueOf(request.getRole());

        employee.setRole(newRole);
        Employee updated = employeeRepository.save(employee);
        log.info("Employee role changed: id={}, oldRole={}, newRole={}", id, oldRole, newRole);

        return new EmployeeResponse(updated);
    }

    /**
     * Activate employee
     */
    @Transactional
    public EmployeeResponse activateEmployee(Long id) {
        log.info("Activating employee: id={}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        if (employee.getActive()) {
            log.warn("Employee already active: id={}", id);
            return new EmployeeResponse(employee);
        }

        employee.setActive(true);
        Employee updated = employeeRepository.save(employee);
        log.info("Employee activated: id={}", id);

        return new EmployeeResponse(updated);
    }

    /**
     * Deactivate employee
     */
    @Transactional
    public EmployeeResponse deactivateEmployee(Long id) {
        log.info("Deactivating employee: id={}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        if (!employee.getActive()) {
            log.warn("Employee already inactive: id={}", id);
            return new EmployeeResponse(employee);
        }

        employee.setActive(false);
        // Clear refresh token to invalidate existing sessions
        employee.setRefreshToken(null);
        employee.setRefreshTokenExpiresAt(null);

        Employee updated = employeeRepository.save(employee);
        log.info("Employee deactivated: id={}", id);

        return new EmployeeResponse(updated);
    }

    /**
     * Reset employee password
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        log.info("Resetting password for employee: id={}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        employee.setPassword(passwordEncoder.encode(newPassword));
        // Invalidate refresh tokens
        employee.setRefreshToken(null);
        employee.setRefreshTokenExpiresAt(null);

        employeeRepository.save(employee);
        log.info("Password reset for employee: id={}", id);
    }

    /**
     * Delete employee
     */
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee: id={}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        String fullName = employee.getFullName();
        employeeRepository.deleteById(id);
        log.info("Employee deleted: id={}, name={}", id, fullName);
    }

    /**
     * Get distinct departments
     */
    @Transactional(readOnly = true)
    public List<String> getDepartments() {
        return employeeRepository.findDistinctDepartments();
    }

    /**
     * Get distinct positions
     */
    @Transactional(readOnly = true)
    public List<String> getPositions() {
        return employeeRepository.findDistinctPositions();
    }

    /**
     * Get employee statistics
     */
    @Transactional(readOnly = true)
    public EmployeeStatistics getStatistics() {
        EmployeeStatistics stats = new EmployeeStatistics();
        stats.setTotalEmployees(employeeRepository.count());
        stats.setActiveEmployees(employeeRepository.countByActiveTrue());
        stats.setAdminCount(employeeRepository.countByRole(EmployeeRole.ADMIN));
        stats.setHrCount(employeeRepository.countByRole(EmployeeRole.HR));
        stats.setManagerCount(employeeRepository.countByRole(EmployeeRole.MANAGER));
        stats.setEmployeeCount(employeeRepository.countByRole(EmployeeRole.EMPLOYEE));
        return stats;
    }
}
