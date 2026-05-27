package com.phungloccoffee.model;

public final class WarehouseSlipType {
    public static final String IMPORT = "IMPORT";
    public static final String EXPORT = "EXPORT";
    public static final String STOCKTAKE = "STOCKTAKE";

    private WarehouseSlipType() {
    }

    public static String toDisplay(String type) {
        return switch (type) {
            case IMPORT -> "Nhập kho";
            case EXPORT -> "Xuất kho";
            case STOCKTAKE -> "Kiểm kê";
            default -> type == null ? "" : type;
        };
    }
}
