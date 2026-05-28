package com.phungloccoffee.model.report;

import java.time.LocalDate;

public record InventoryReportFilter(LocalDate fromDate, LocalDate toDate, String branchId, String categoryId,
                                    InventoryStatus status) {
    public InventoryReportFilter {
        branchId = branchId == null || branchId.isBlank() ? null : branchId;
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId;
    }

    public boolean allBranches() {
        return branchId == null || branchId.isBlank();
    }

    public boolean allCategories() {
        return categoryId == null || categoryId.isBlank();
    }
}
