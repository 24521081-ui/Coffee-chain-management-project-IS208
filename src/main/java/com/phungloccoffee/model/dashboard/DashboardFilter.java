package com.phungloccoffee.model.dashboard;

import java.time.LocalDate;

public record DashboardFilter(LocalDate fromDate, LocalDate toDate, String branchId, PeriodType periodType) {
    public DashboardFilter {
        periodType = periodType == null ? PeriodType.CUSTOM : periodType;
        branchId = branchId == null || branchId.isBlank() ? null : branchId;
    }

    public boolean allBranches() {
        return branchId == null || branchId.isBlank();
    }
}
