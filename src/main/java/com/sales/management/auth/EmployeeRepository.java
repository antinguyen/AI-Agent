package com.sales.management.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Employee entity
 * Provides database operations for employee records
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find employee by username
     */
    Optional<Employee> findByUsername(String username);

    /**
     * Find employee by email
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Check if email exists (excluding a specific employee ID)
     */
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.email = :email AND e.id != :excludeId")
    boolean existsByEmailExcluding(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all active employees
     */
    List<Employee> findByActiveTrue();

    /**
     * Find employees by department
     */
    List<Employee> findByDepartment(String department);

    /**
     * Find employees by position
     */
    List<Employee> findByPosition(String position);

    /**
     * Find employees by role
     */
    List<Employee> findByRole(EmployeeRole role);

    /**
     * Find employees with pagination and complex filters
     */
    @Query("SELECT e FROM Employee e WHERE " +
           "(:username IS NULL OR LOWER(e.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:firstName IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:email IS NULL OR LOWER(e.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:department IS NULL OR e.department = :department) AND " +
           "(:position IS NULL OR e.position = :position) AND " +
           "(:role IS NULL OR e.role = :role) AND " +
           "(:active IS NULL OR e.active = :active)")
    Page<Employee> findEmployees(
        @Param("username") String username,
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("email") String email,
        @Param("department") String department,
        @Param("position") String position,
        @Param("role") EmployeeRole role,
        @Param("active") Boolean active,
        Pageable pageable
    );

    /**
     * Get distinct departments
     */
    @Query("SELECT DISTINCT e.department FROM Employee e ORDER BY e.department")
    List<String> findDistinctDepartments();

    /**
     * Get distinct positions
     */
    @Query("SELECT DISTINCT e.position FROM Employee e ORDER BY e.position")
    List<String> findDistinctPositions();

    /**
     * Count employees by role
     */
    long countByRole(EmployeeRole role);

    /**
     * Count active employees
     */
    long countByActiveTrue();

    /**
     * Count employees by department
     */
    long countByDepartment(String department);
}
