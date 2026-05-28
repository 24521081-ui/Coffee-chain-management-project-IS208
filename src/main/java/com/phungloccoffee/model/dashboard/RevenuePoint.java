package com.phungloccoffee.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePoint(LocalDate date, BigDecimal revenue) {
    public RevenuePoint {
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
