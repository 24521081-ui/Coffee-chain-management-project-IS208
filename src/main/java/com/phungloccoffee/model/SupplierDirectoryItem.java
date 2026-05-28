package com.phungloccoffee.model;

public class SupplierDirectoryItem {
    private final String supplierId;
    private final String supplierName;
    private final String category;
    private final String contact;
    private final String phone;
    private final String email;
    private final int status;

    public SupplierDirectoryItem(String supplierId, String supplierName, String category,
                                 String contact, String phone, String email, int status) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.category = category;
        this.contact = contact;
        this.phone = phone;
        this.email = email;
        this.status = status;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getCategory() {
        return category;
    }

    public String getContact() {
        return contact;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public int getStatus() {
        return status;
    }
}
