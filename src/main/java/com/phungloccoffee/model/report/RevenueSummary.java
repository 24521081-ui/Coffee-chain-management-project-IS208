package com.phungloccoffee.model.report;

import java.math.BigDecimal;

public record RevenueSummary(BigDecimal totalRevenue, int totalOrders, BigDecimal averageOrderValue,
                             BigDecimal growthPercent, String growthLabel) {
    public RevenueSummary {
        totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        averageOrderValue = averageOrderValue == null ? BigDecimal.ZERO : averageOrderValue;
        growthPercent = growthPercent == null ? BigDecimal.ZERO : growthPercent;
        growthLabel = growthLabel == null ? "0%" : growthLabel;
    }
}
