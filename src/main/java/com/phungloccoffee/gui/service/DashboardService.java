package com.phungloccoffee.gui.service;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.BestSellerReportService.BestSellerReportData;
import com.phungloccoffee.gui.service.InventoryReportService.InventoryReportData;
import com.phungloccoffee.gui.service.RevenueReportService.RevenueReportData;
import com.phungloccoffee.model.report.ReportModels.BranchOption;

import java.time.LocalDate;
import java.util.List;

import static com.phungloccoffee.model.report.ReportModels.ALL_CATEGORY;
import static com.phungloccoffee.model.report.ReportModels.ALL_STATUS;

public class DashboardService {
    private final RevenueReportService revenueReportService = new RevenueReportService();
    private final BestSellerReportService bestSellerReportService = new BestSellerReportService();
    private final InventoryReportService inventoryReportService = new InventoryReportService();

    public List<BranchOption> loadBranchOptions() throws DatabaseException {
        return revenueReportService.loadBranchOptions();
    }

    public DashboardData loadDashboard(LocalDate fromDate, LocalDate toDate, String branchId) throws DatabaseException {
        return new DashboardData(
                revenueReportService.loadReport(fromDate, toDate, branchId),
                bestSellerReportService.loadReport(fromDate, toDate, branchId, ALL_CATEGORY),
                inventoryReportService.loadReport(fromDate, toDate, branchId, ALL_CATEGORY, ALL_STATUS)
        );
    }

    public record DashboardData(RevenueReportData revenue, BestSellerReportData bestSeller,
                                InventoryReportData inventory) {
    }
}
