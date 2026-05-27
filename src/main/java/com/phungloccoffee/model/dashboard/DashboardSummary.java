package com.phungloccoffee.model.dashboard;

import java.math.BigDecimal;

public record DashboardSummary(BigDecimal revenue, int orderCount, BigDecimal productsSold, int inventoryWarnings) {
    public DashboardSummary {
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
        productsSold = productsSold == null ? BigDecimal.ZERO : productsSold;
    }
}
