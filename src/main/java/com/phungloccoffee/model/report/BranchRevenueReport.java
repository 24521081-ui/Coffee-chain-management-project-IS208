package com.phungloccoffee.model.report;

import java.math.BigDecimal;

public record BranchRevenueReport(String branchId, String branchName, String chartLabel, BigDecimal revenue,
                                  int orderCount, BigDecimal averageOrderValue, BigDecimal growthPercent,
                                  String growthLabel, BranchRevenueStatus status) {
    public BranchRevenueReport {
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
        averageOrderValue = averageOrderValue == null ? BigDecimal.ZERO : averageOrderValue;
        growthPercent = growthPercent == null ? BigDecimal.ZERO : growthPercent;
        growthLabel = growthLabel == null ? "0%" : growthLabel;
    }
}
