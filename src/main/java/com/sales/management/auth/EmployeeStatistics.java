package com.sales.management.auth;

/**
 * DTO for employee statistics
 */
public class EmployeeStatistics {
    private long totalEmployees;
    private long activeEmployees;
    private long adminCount;
    private long hrCount;
    private long managerCount;
    private long employeeCount;

    public EmployeeStatistics() {
    }

    public EmployeeStatistics(long totalEmployees, long activeEmployees, long adminCount, long hrCount,
                              long managerCount, long employeeCount) {
        this.totalEmployees = totalEmployees;
        this.activeEmployees = activeEmployees;
        this.adminCount = adminCount;
        this.hrCount = hrCount;
        this.managerCount = managerCount;
        this.employeeCount = employeeCount;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(long activeEmployees) {
        this.activeEmployees = activeEmployees;
    }

    public long getAdminCount() {
        return adminCount;
    }

    public void setAdminCount(long adminCount) {
        this.adminCount = adminCount;
    }

    public long getHrCount() {
        return hrCount;
    }

    public void setHrCount(long hrCount) {
        this.hrCount = hrCount;
    }

    public long getManagerCount() {
        return managerCount;
    }

    public void setManagerCount(long managerCount) {
        this.managerCount = managerCount;
    }

    public long getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(long employeeCount) {
        this.employeeCount = employeeCount;
    }
}
