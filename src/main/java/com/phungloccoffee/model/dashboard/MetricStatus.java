package com.phungloccoffee.model.dashboard;

public enum MetricStatus {
    GOOD("Tốt"),
    WARNING("Cảnh báo"),
    DANGER("Nguy hiểm"),
    NEW("Mới");

    private final String displayName;

    MetricStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
