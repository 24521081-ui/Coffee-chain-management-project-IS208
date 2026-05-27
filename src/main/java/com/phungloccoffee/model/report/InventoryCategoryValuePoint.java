package com.phungloccoffee.model.report;

import java.math.BigDecimal;

public record InventoryCategoryValuePoint(String categoryId, String categoryName, BigDecimal inventoryValue) {
    public InventoryCategoryValuePoint {
        inventoryValue = inventoryValue == null ? BigDecimal.ZERO : inventoryValue;
    }
}
