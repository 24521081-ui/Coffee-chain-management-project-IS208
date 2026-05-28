package com.phungloccoffee.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueTrendPoint(String label, LocalDate periodStart, BigDecimal revenue) {
    public RevenueTrendPoint {
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
