package com.phungloccoffee.gui.service;

import com.phungloccoffee.dao.InventoryReportDAO;
import com.phungloccoffee.dao.ReportLookupDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.InventoryBranch;
import com.phungloccoffee.model.report.ReportModels.InventoryCategory;
import com.phungloccoffee.model.report.ReportModels.InventoryItem;
import com.phungloccoffee.model.report.ReportModels.InventorySummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.phungloccoffee.model.report.ReportModels.ALL_STATUS;

public class InventoryReportService {
    private final ReportLookupDAO lookupDAO = new ReportLookupDAO();
    private final InventoryReportDAO inventoryReportDAO = new InventoryReportDAO();

    public List<BranchOption> loadBranchOptions() throws DatabaseException {
        return lookupDAO.findBranchOptions();
    }

    public List<String> loadCategories() throws DatabaseException {
        return lookupDAO.findProductCategories();
    }

    public List<String> loadStatuses() {
        return List.of(ALL_STATUS, "Ổn định", "Tồn thấp", "Hết hàng");
    }

    public InventoryReportData loadReport(LocalDate fromDate, LocalDate toDate, String branchId,
                                          String categoryName, String status) throws DatabaseException {
        List<InventoryItem> items = inventoryReportDAO.findInventoryItems(fromDate, toDate, branchId, categoryName, status);
        return new InventoryReportData(
                summary(items),
                branchSummary(items),
                categorySummary(items),
                items
        );
    }

    private InventorySummary summary(List<InventoryItem> items) {
        int tracked = items.size();
        int lowStock = (int) items.stream().filter(item -> "Tồn thấp".equals(item.status())).count();
        int outOfStock = (int) items.stream().filter(item -> "Hết hàng".equals(item.status())).count();
        BigDecimal value = items.stream().map(InventoryItem::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InventorySummary(tracked, lowStock, outOfStock, value);
    }

    private List<InventoryBranch> branchSummary(List<InventoryItem> items) {
        Map<String, List<InventoryItem>> byBranch = items.stream()
                .collect(Collectors.groupingBy(InventoryItem::branchId, LinkedHashMap::new, Collectors.toList()));
        return byBranch.entrySet().stream()
                .map(entry -> {
                    List<InventoryItem> branchItems = entry.getValue();
                    String branchName = branchItems.getFirst().branchName();
                    return new InventoryBranch(
                            entry.getKey(),
                            branchName,
                            ReportLookupDAO.chartLabel(branchName),
                            branchName,
                            branchItems.size(),
                            (int) branchItems.stream().filter(item -> "Tồn thấp".equals(item.status())).count(),
                            (int) branchItems.stream().filter(item -> "Hết hàng".equals(item.status())).count(),
                            branchItems.stream().map(InventoryItem::value).reduce(BigDecimal.ZERO, BigDecimal::add)
                    );
                })
                .sorted(Comparator.comparingInt(InventoryBranch::warningCount).reversed())
                .toList();
    }

    private List<InventoryCategory> categorySummary(List<InventoryItem> items) {
        return items.stream()
                .collect(Collectors.groupingBy(InventoryItem::categoryName, LinkedHashMap::new,
                        Collectors.mapping(InventoryItem::value, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet()
                .stream()
                .map(entry -> new InventoryCategory(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record InventoryReportData(InventorySummary summary, List<InventoryBranch> branches,
                                      List<InventoryCategory> categories, List<InventoryItem> items) {
    }
}
