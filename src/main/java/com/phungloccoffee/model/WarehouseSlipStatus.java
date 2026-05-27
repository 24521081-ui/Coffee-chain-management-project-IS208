package com.phungloccoffee.model;

public final class WarehouseSlipStatus {
    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private WarehouseSlipStatus() {
    }

    public static String toDisplay(String status) {
        return switch (status) {
            case DRAFT -> "Nháp";
            case PENDING_APPROVAL -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
            default -> status == null ? "" : status;
        };
    }
}
