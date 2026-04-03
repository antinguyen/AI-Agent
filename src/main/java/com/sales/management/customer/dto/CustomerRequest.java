package com.sales.management.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CustomerRequest {

    @NotBlank(message = "code is required")
    @Size(max = 32, message = "code max length is 32")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name max length is 255")
    private String name;

    @Size(max = 64, message = "phone max length is 64")
    private String phone;

    @Email(message = "email is invalid")
    @Size(max = 255, message = "email max length is 255")
    private String email;

    @NotNull(message = "active is required")
    private Boolean active;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
