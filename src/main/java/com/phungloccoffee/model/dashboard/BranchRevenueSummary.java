package com.phungloccoffee.model.dashboard;

import java.math.BigDecimal;

public record BranchRevenueSummary(String branchId, String branchName, BigDecimal revenue) {
    public BranchRevenueSummary {
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
