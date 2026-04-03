package com.sales.management.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WarehouseCreateRequest {
    @NotBlank(message = "Code là bắt buộc")
    @Size(min = 2, max = 64, message = "Code phải từ 2-64 ký tự")
    public String code;

    @NotBlank(message = "Tên kho là bắt buộc")
    @Size(min = 2, max = 255, message = "Tên phải từ 2-255 ký tự")
    public String name;

    public String address;
    public Boolean active = true;

    public WarehouseCreateRequest() {
    }

    public WarehouseCreateRequest(String code, String name, String address, Boolean active) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.active = active;
    }
}
