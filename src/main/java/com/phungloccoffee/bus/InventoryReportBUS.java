package com.phungloccoffee.bus;

import com.phungloccoffee.dao.InventoryReportDAO;
import com.phungloccoffee.dao.InventoryReportDAO.InventoryBranchAlertRaw;
import com.phungloccoffee.dao.InventoryReportDAO.InventoryCategoryValueRaw;
import com.phungloccoffee.dao.InventoryReportDAO.InventoryItemRaw;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.InventoryBranchAlertPoint;
import com.phungloccoffee.model.report.InventoryCategoryOption;
import com.phungloccoffee.model.report.InventoryCategoryValuePoint;
import com.phungloccoffee.model.report.InventoryItemReport;
import com.phungloccoffee.model.report.InventoryReportData;
import com.phungloccoffee.model.report.InventoryReportFilter;
import com.phungloccoffee.model.report.InventoryStatus;
import com.phungloccoffee.model.report.InventorySummary;
import com.phungloccoffee.model.report.ReportModels.BranchOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class InventoryReportBUS {
    private final InventoryReportDAO inventoryReportDAO = new InventoryReportDAO();

    public List<BranchOption> getBranchOptions() throws DatabaseException {
        return inventoryReportDAO.findBranchOptions();
    }

    public List<InventoryCategoryOption> getCategoryOptions() throws DatabaseException {
        return inventoryReportDAO.findCategoryOptions();
    }

    public LocalDate getDefaultReportDate() throws DatabaseException {
        return inventoryReportDAO.findLatestInventoryDate();
    }

    public InventoryReportData getInventoryReport(InventoryReportFilter filter) throws DatabaseException {
        validateFilter(filter);

        List<InventoryItemRaw> currentRows = inventoryReportDAO.findInventoryItems(filter);
        List<InventoryItemRaw> previousRows = inventoryReportDAO.findInventoryItems(previousFilter(filter));
        List<InventoryBranchAlertPoint> branchAlerts = inventoryReportDAO.findBranchAlerts(filter).stream()
                .map(this::toBranchAlertPoint)
                .toList();
        List<InventoryCategoryValuePoint> categoryValues = inventoryReportDAO.findCategoryValues(filter).stream()
                .map(this::toCategoryValuePoint)
                .toList();
        List<InventoryItemReport> items = currentRows.stream()
                .map(this::toItemReport)
                .toList();

        return new InventoryReportData(
                buildSummary(filter, currentRows, previousRows, branchAlerts),
                branchAlerts,
                categoryValues,
                items,
                filter.allCategories() ? "Cơ cấu tồn kho theo nhóm" : "Giá trị tồn kho nhóm đang chọn"
        );
    }

    private void validateFilter(InventoryReportFilter filter) {
        if (filter == null || filter.fromDate() == null || filter.toDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ khoảng thời gian báo cáo");
        }
        if (filter.fromDate().isAfter(filter.toDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
    }

    private InventoryReportFilter previousFilter(InventoryReportFilter filter) {
        long days = ChronoUnit.DAYS.between(filter.fromDate(), filter.toDate()) + 1;
        LocalDate previousTo = filter.fromDate().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        return new InventoryReportFilter(previousFrom, previousTo, filter.branchId(), filter.categoryId(), filter.status());
    }

    private InventorySummary buildSummary(InventoryReportFilter filter, List<InventoryItemRaw> currentRows,
                                          List<InventoryItemRaw> previousRows,
                                          List<InventoryBranchAlertPoint> branchAlerts) {
        int trackedProducts = filter.allBranches()
                ? (int) currentRows.stream().map(InventoryItemRaw::productId).filter(Objects::nonNull).distinct().count()
                : currentRows.size();
        int alertBranchCount = (int) branchAlerts.stream().filter(point -> point.alertCount() > 0).count();
        int lowStockCount = (int) currentRows.stream().filter(row -> row.quantityOnHand().compareTo(row.minQuantity()) <= 0).count();
        BigDecimal currentValue = totalValue(currentRows);
        BigDecimal previousValue = totalValue(previousRows);
        BigDecimal growth = growthPercent(currentValue, previousValue);
        return new InventorySummary(
                trackedProducts,
                alertBranchCount,
                lowStockCount,
                currentValue,
                growth,
                growthLabel(currentValue, previousValue)
        );
    }

    private InventoryItemReport toItemReport(InventoryItemRaw row) {
        return new InventoryItemReport(
                row.productId(),
                row.productName(),
                row.branchId(),
                row.branchName(),
                row.categoryId(),
                row.categoryName(),
                row.quantityOnHand(),
                row.minQuantity(),
                row.inventoryValue(),
                statusFromQuantity(row.quantityOnHand(), row.minQuantity())
        );
    }

    private InventoryBranchAlertPoint toBranchAlertPoint(InventoryBranchAlertRaw row) {
        return new InventoryBranchAlertPoint(row.branchId(), row.branchName(), row.chartLabel(), row.alertCount());
    }

    private InventoryCategoryValuePoint toCategoryValuePoint(InventoryCategoryValueRaw row) {
        return new InventoryCategoryValuePoint(row.categoryId(), row.categoryName(), row.inventoryValue());
    }

    private InventoryStatus statusFromQuantity(BigDecimal quantityOnHand, BigDecimal minQuantity) {
        if (quantityOnHand.compareTo(BigDecimal.ZERO) <= 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
        if (quantityOnHand.compareTo(minQuantity) <= 0) {
            return InventoryStatus.LOW_STOCK;
        }
        return InventoryStatus.STABLE;
    }

    private BigDecimal totalValue(List<InventoryItemRaw> rows) {
        return rows.stream().map(InventoryItemRaw::inventoryValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal growthPercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private String growthLabel(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal growth = growthPercent(current, previous).setScale(0, RoundingMode.HALF_UP);
        return (growth.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + growth.toPlainString() + "%";
    }
}
