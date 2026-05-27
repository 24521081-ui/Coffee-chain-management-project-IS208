package com.phungloccoffee.model;

public class CustomerSummary {
    private final int totalCustomers;
    private final int newCustomersThisMonth;

    public CustomerSummary(int totalCustomers, int newCustomersThisMonth) {
        this.totalCustomers = totalCustomers;
        this.newCustomersThisMonth = newCustomersThisMonth;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public int getNewCustomersThisMonth() {
        return newCustomersThisMonth;
    }
}
