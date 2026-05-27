package com.phungloccoffee.model;

import java.util.List;

public class CustomerPageResult {
    private final List<KhachHang> customers;
    private final int currentPage;
    private final int pageSize;
    private final int totalRows;
    private final int totalPages;

    public CustomerPageResult(List<KhachHang> customers, int currentPage, int pageSize, int totalRows, int totalPages) {
        this.customers = customers;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalRows = totalRows;
        this.totalPages = totalPages;
    }

    public List<KhachHang> getCustomers() {
        return customers;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
