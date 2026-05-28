package com.phungloccoffee.model.report;

public enum RevenuePeriodType {
    DAY("Ngày"),
    MONTH("Tháng"),
    QUARTER("Quý"),
    YEAR("Năm"),
    CUSTOM("Tùy chọn");

    private final String displayName;

    RevenuePeriodType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
