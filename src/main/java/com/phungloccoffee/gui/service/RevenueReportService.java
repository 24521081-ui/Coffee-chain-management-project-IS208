package com.phungloccoffee.gui.service;

import com.phungloccoffee.dao.ReportLookupDAO;
import com.phungloccoffee.dao.RevenueReportDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.BranchRevenue;
import com.phungloccoffee.model.report.ReportModels.DailyRevenue;
import com.phungloccoffee.model.report.ReportModels.RevenueSummary;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RevenueReportService {
    private final ReportLookupDAO lookupDAO = new ReportLookupDAO();
    private final RevenueReportDAO revenueReportDAO = new RevenueReportDAO();

    public List<BranchOption> loadBranchOptions() throws DatabaseException {
        return lookupDAO.findBranchOptions();
    }

    public RevenueReportData loadReport(LocalDate fromDate, LocalDate toDate, String branchId) throws DatabaseException {
        RevenueSummary current = revenueReportDAO.findSummary(fromDate, toDate, branchId);
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousTo = fromDate.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        RevenueSummary previous = revenueReportDAO.findSummary(previousFrom, previousTo, branchId);
        RevenueSummary summary = new RevenueSummary(
                current.revenue(),
                current.orders(),
                current.productsSold(),
                ReportFilterUtils.percentChange(current.revenue(), previous.revenue())
        );
        return new RevenueReportData(
                summary,
                revenueReportDAO.findRevenueByDate(fromDate, toDate, branchId),
                revenueReportDAO.findRevenueByBranch(fromDate, toDate, branchId)
        );
    }

    public record RevenueReportData(RevenueSummary summary, List<DailyRevenue> dailyRevenue,
                                    List<BranchRevenue> branchRevenue) {
    }
}
