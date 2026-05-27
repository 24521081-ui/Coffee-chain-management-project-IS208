package com.phungloccoffee.model.dashboard;

import java.util.List;

public record DashboardData(
        DashboardFilter filter,
        DashboardKpi revenueKpi,
        DashboardKpi orderKpi,
        DashboardKpi productKpi,
        DashboardKpi inventoryWarningKpi,
        List<RevenuePoint> revenueByDate,
        String periodLabel,
        String branchOverviewLabel,
        BranchRevenueSummary topBranch,
        TopProductSummary topProduct,
        List<DashboardMetric> activityMetrics
) {
}
