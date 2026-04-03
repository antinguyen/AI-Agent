package com.sales.management.warehouse.dto;

public class WarehouseResponse {
    public final Long id;
    public final String code;
    public final String name;
    public final String address;
    public final Boolean active;
    public final String createdAt;
    public final String createdBy;

    public WarehouseResponse(Long id, String code, String name, String address, Boolean active, String createdAt, String createdBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.address = address;
        this.active = active;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
}
