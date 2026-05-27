package com.phungloccoffee.model.report;

public record InventoryCategoryOption(String id, String displayName) {
    public boolean isAll() {
        return id == null || id.isBlank();
    }
}
