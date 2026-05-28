package com.phungloccoffee.model.dashboard;

import java.math.BigDecimal;

public record DashboardKpi(BigDecimal value, String formattedChange, MetricStatus status, String note) {
    public DashboardKpi {
        value = value == null ? BigDecimal.ZERO : value;
    }
}
