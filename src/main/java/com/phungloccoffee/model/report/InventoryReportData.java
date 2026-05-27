package com.phungloccoffee.model.report;

import java.util.List;

public record InventoryReportData(InventorySummary summary, List<InventoryBranchAlertPoint> branchAlerts,
                                  List<InventoryCategoryValuePoint> categoryValues,
                                  List<InventoryItemReport> items,
                                  String categoryChartTitle) {
}
