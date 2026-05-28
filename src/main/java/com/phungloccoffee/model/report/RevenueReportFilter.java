package com.phungloccoffee.model.report;

import java.time.LocalDate;

public record RevenueReportFilter(RevenuePeriodType periodType, LocalDate fromDate, LocalDate toDate, String branchId) {
    public RevenueReportFilter {
        periodType = periodType == null ? RevenuePeriodType.DAY : periodType;
        branchId = branchId == null || branchId.isBlank() ? null : branchId;
    }

    public boolean allBranches() {
        return branchId == null || branchId.isBlank();
    }
}
