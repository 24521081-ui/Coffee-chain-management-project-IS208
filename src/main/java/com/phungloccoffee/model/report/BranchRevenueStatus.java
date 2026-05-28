package com.phungloccoffee.model.report;

public enum BranchRevenueStatus {
    TOT("T\u1ed1t"),
    ON_DINH("\u1ed4n \u0111\u1ecbnh"),
    CANH_BAO("C\u1ea3nh b\u00e1o"),
    KEM("K\u00e9m"),
    NGUY_HIEM("Nguy hi\u1ec3m");

    private final String displayName;

    BranchRevenueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
