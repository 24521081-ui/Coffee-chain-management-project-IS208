package com.phungloccoffee.model.report;

import java.math.BigDecimal;

public record InventorySummary(int totalTrackedProducts, int alertBranchCount, int lowStockItemCount,
                               BigDecimal totalInventoryValue, BigDecimal growthPercent, String growthLabel) {
    public InventorySummary {
        totalInventoryValue = totalInventoryValue == null ? BigDecimal.ZERO : totalInventoryValue;
        growthPercent = growthPercent == null ? BigDecimal.ZERO : growthPercent;
        growthLabel = growthLabel == null ? "0%" : growthLabel;
    }
}
