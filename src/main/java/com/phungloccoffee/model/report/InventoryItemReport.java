package com.phungloccoffee.model.report;

import java.math.BigDecimal;

public record InventoryItemReport(String productId, String productName, String branchId, String branchName,
                                  String categoryId, String categoryName, BigDecimal quantityOnHand,
                                  BigDecimal minQuantity, BigDecimal inventoryValue, InventoryStatus status) {
    public InventoryItemReport {
        quantityOnHand = quantityOnHand == null ? BigDecimal.ZERO : quantityOnHand;
        minQuantity = minQuantity == null ? BigDecimal.ZERO : minQuantity;
        inventoryValue = inventoryValue == null ? BigDecimal.ZERO : inventoryValue;
    }
}
