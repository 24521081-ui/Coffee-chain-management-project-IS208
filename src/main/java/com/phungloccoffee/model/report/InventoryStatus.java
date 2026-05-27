package com.phungloccoffee.model.report;

public enum InventoryStatus {
    OUT_OF_STOCK("H\u1ebft h\u00e0ng"),
    LOW_STOCK("T\u1ed3n th\u1ea5p"),
    STABLE("\u1ed4n \u0111\u1ecbnh");

    private final String displayName;

    InventoryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
