package com.sales.management.auth;

/**
 * Employee Role Enumeration
 * Defines available roles for employee access control
 */
public enum EmployeeRole {
    /**
     * Full system administrator with all permissions
     */
    ADMIN,

    /**
     * Human Resources staff - can manage employees and roles
     */
    HR,

    /**
     * Manager - can view team details and update own profile
     */
    MANAGER,

    /**
     * Regular employee - can view own profile
     */
    EMPLOYEE
}
