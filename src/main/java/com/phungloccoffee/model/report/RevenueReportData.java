package com.phungloccoffee.model.report;

import java.util.List;

public record RevenueReportData(RevenueSummary summary, List<RevenueTrendPoint> trendPoints,
                                List<BranchRevenueReport> branchReports,
                                List<BranchRevenueReport> topBranches,
                                String trendTitle,
                                String trendSubtitle,
                                String branchChartTitle,
                                String branchChartSubtitle) {
}
