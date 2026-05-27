package com.phungloccoffee.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ReportModels {
    public static final String ALL_BRANCH_ID = "";
    public static final String ALL_CATEGORY = "Tất cả nhóm";
    public static final String ALL_STATUS = "Tất cả trạng thái";

    private ReportModels() {
    }

    public record BranchOption(String id, String displayName, String chartLabel, String fullName) {
        public boolean isAll() {
            return id == null || id.isBlank();
        }
    }

    public record RevenueSummary(BigDecimal revenue, int orders, BigDecimal productsSold, int growthPercent) {
    }

    public record DailyRevenue(LocalDate date, BigDecimal revenue, int orders, BigDecimal productsSold) {
    }

    public record BranchRevenue(String branchId, String displayName, String chartLabel, String fullName,
                                BigDecimal revenue, int orders, BigDecimal productsSold) {
    }

    public record ProductSale(String productId, String productName, String chartLabel, String categoryName,
                              BigDecimal quantity, BigDecimal revenue, int sharePercent, int trendPercent) {
    }

    public record ProductSummary(BigDecimal quantity, BigDecimal revenue, ProductSale topProduct) {
    }

    public record InventoryBranch(String branchId, String displayName, String chartLabel, String fullName,
                                  int tracked, int lowStock, int outOfStock, BigDecimal value) {
        public int warningCount() {
            return lowStock + outOfStock;
        }

        public String status() {
            if (outOfStock > 0) {
                return "Cảnh báo";
            }
            if (lowStock > 0) {
                return "Cần kiểm tra";
            }
            return "Ổn định";
        }
    }

    public record InventoryCategory(String category, BigDecimal value) {
    }

    public record InventoryItem(String branchId, String branchName, String productId, String productName,
                                String categoryName, BigDecimal currentQuantity, BigDecimal minQuantity,
                                BigDecimal value, String status) {
    }

    public record InventorySummary(int tracked, int lowStock, int outOfStock, BigDecimal value) {
    }
}
