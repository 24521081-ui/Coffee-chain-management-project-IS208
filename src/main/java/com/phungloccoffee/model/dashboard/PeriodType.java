package com.phungloccoffee.model.dashboard;

public enum PeriodType {
    TODAY("Hôm nay"),
    THIS_WEEK("Tuần này"),
    THIS_MONTH("Tháng này"),
    THIS_YEAR("Năm này"),
    CUSTOM("Tùy chọn");

    private final String displayName;

    PeriodType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
