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

    @Size(max = 500, message = "address max length is 500")
    private String address;

    @Size(max = 32, message = "taxCode max length is 32")
    private String taxCode;

    @Size(max = 255, message = "legalRepresentative max length is 255")
    private String legalRepresentative;

    @Size(max = 255, message = "contactPerson max length is 255")
    private String contactPerson;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getLegalRepresentative() {
        return legalRepresentative;
    }

    public void setLegalRepresentative(String legalRepresentative) {
        this.legalRepresentative = legalRepresentative;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
}
