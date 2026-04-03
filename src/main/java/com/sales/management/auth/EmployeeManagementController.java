package com.sales.management.auth;

import com.sales.management.auth.dto.CreateEmployeeRequest;
import com.sales.management.auth.dto.EmployeeResponse;
import com.sales.management.auth.dto.UpdateEmployeeRequest;
import com.sales.management.auth.dto.UpdateEmployeeRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * REST Controller for Employee Management
 * Provides endpoints for creating, reading, updating, and deleting employees
 */
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee Management", description = "Endpoints for managing employees")
@SecurityRequirement(name = "Bearer Authentication")
public class EmployeeManagementController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeManagementController.class);

    private final EmployeeManagementService employeeService;

    public EmployeeManagementController(EmployeeManagementService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Create a new employee
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create new employee", description = "Create a new employee (requires ADMIN or HR role)")
    @ApiResponse(responseCode = "201", description = "Employee created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        log.info("POST /api/v1/employees - Creating employee: {}", request.getUsername());
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get employee by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get employee by ID", description = "Retrieve employee details by ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeResponse> getEmployee(
        @PathVariable @Parameter(description = "Employee ID") Long id) {
        log.info("GET /api/v1/employees/{} - Fetching employee", id);
        EmployeeResponse response = employeeService.getEmployee(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List employees with pagination and filters
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "List employees", description = "Retrieve employees with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Employees retrieved successfully")
    public ResponseEntity<Page<EmployeeResponse>> listEmployees(
        @RequestParam(required = false) @Parameter(description = "Username filter") String username,
        @RequestParam(required = false) @Parameter(description = "First name filter") String firstName,
        @RequestParam(required = false) @Parameter(description = "Last name filter") String lastName,
        @RequestParam(required = false) @Parameter(description = "Email filter") String email,
        @RequestParam(required = false) @Parameter(description = "Department filter") String department,
        @RequestParam(required = false) @Parameter(description = "Position filter") String position,
        @RequestParam(required = false) @Parameter(description = "Role filter (ADMIN|HR|MANAGER|EMPLOYEE)") String role,
        @RequestParam(required = false) @Parameter(description = "Active status filter") Boolean active,
        @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/v1/employees - Listing employees with filters");
        Page<EmployeeResponse> response = employeeService.listEmployees(
            username, firstName, lastName, email, department, position, role, active, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Update employee profile
     */
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update employee profile", description = "Update employee profile information")
    @ApiResponse(responseCode = "200", description = "Employee profile updated successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public ResponseEntity<EmployeeResponse> updateEmployee(
        @PathVariable @Parameter(description = "Employee ID") Long id,
        @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("PUT /api/v1/employees/{}/profile - Updating employee profile", id);
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Change employee role
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Change employee role", description = "Update employee role (requires ADMIN or HR)")
    @ApiResponse(responseCode = "200", description = "Role updated successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeResponse> changeRole(
        @PathVariable @Parameter(description = "Employee ID") Long id,
        @Valid @RequestBody UpdateEmployeeRoleRequest request) {
        log.info("PUT /api/v1/employees/{}/role - Changing role to {}", id, request.getRole());
        EmployeeResponse response = employeeService.changeRole(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate employee
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Activate employee", description = "Activate an inactive employee")
    @ApiResponse(responseCode = "200", description = "Employee activated successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeResponse> activateEmployee(
        @PathVariable @Parameter(description = "Employee ID") Long id) {
        log.info("PUT /api/v1/employees/{}/activate - Activating employee", id);
        EmployeeResponse response = employeeService.activateEmployee(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate employee
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Deactivate employee", description = "Deactivate an active employee")
    @ApiResponse(responseCode = "200", description = "Employee deactivated successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(
        @PathVariable @Parameter(description = "Employee ID") Long id) {
        log.info("PUT /api/v1/employees/{}/deactivate - Deactivating employee", id);
        EmployeeResponse response = employeeService.deactivateEmployee(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete employee
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete employee", description = "Permanently delete an employee (requires ADMIN)")
    @ApiResponse(responseCode = "204", description = "Employee deleted successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    public ResponseEntity<Void> deleteEmployee(
        @PathVariable @Parameter(description = "Employee ID") Long id) {
        log.info("DELETE /api/v1/employees/{} - Deleting employee", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get distinct departments
     */
    @GetMapping("/metadata/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get departments", description = "Retrieve list of distinct departments")
    @ApiResponse(responseCode = "200", description = "Departments retrieved successfully")
    public ResponseEntity<List<String>> getDepartments() {
        log.info("GET /api/v1/employees/metadata/departments - Fetching departments");
        List<String> departments = employeeService.getDepartments();
        return ResponseEntity.ok(departments);
    }

    /**
     * Get distinct positions
     */
    @GetMapping("/metadata/positions")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get positions", description = "Retrieve list of distinct positions")
    @ApiResponse(responseCode = "200", description = "Positions retrieved successfully")
    public ResponseEntity<List<String>> getPositions() {
        log.info("GET /api/v1/employees/metadata/positions - Fetching positions");
        List<String> positions = employeeService.getPositions();
        return ResponseEntity.ok(positions);
    }

    /**
     * Get employee statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get statistics", description = "Retrieve employee statistics")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<EmployeeStatistics> getStatistics() {
        log.info("GET /api/v1/employees/statistics - Fetching employee statistics");
        EmployeeStatistics stats = employeeService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}
