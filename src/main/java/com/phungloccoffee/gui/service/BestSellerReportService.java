package com.phungloccoffee.gui.service;

import com.phungloccoffee.dao.BestSellerReportDAO;
import com.phungloccoffee.dao.ReportLookupDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.ProductSale;
import com.phungloccoffee.model.report.ReportModels.ProductSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BestSellerReportService {
    private final ReportLookupDAO lookupDAO = new ReportLookupDAO();
    private final BestSellerReportDAO bestSellerReportDAO = new BestSellerReportDAO();

    public List<BranchOption> loadBranchOptions() throws DatabaseException {
        return lookupDAO.findBranchOptions();
    }

    public List<String> loadCategories() throws DatabaseException {
        return lookupDAO.findProductCategories();
    }

    public BestSellerReportData loadReport(LocalDate fromDate, LocalDate toDate, String branchId,
                                           String categoryName) throws DatabaseException {
        List<ProductSale> currentRows = bestSellerReportDAO.findTopProducts(fromDate, toDate, branchId, categoryName, 50);
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousTo = fromDate.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        Map<String, BigDecimal> previousQuantityByProduct = bestSellerReportDAO
                .findTopProducts(previousFrom, previousTo, branchId, categoryName, 500)
                .stream()
                .collect(Collectors.toMap(ProductSale::productId, ProductSale::quantity));
        BigDecimal totalQuantity = currentRows.stream()
                .map(ProductSale::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ProductSale> rows = currentRows.stream()
                .map(row -> enrich(row, totalQuantity, previousQuantityByProduct.get(row.productId())))
                .toList();
        BigDecimal totalRevenue = rows.stream()
                .map(ProductSale::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ProductSummary summary = new ProductSummary(totalQuantity, totalRevenue, rows.stream().findFirst().orElse(null));
        return new BestSellerReportData(summary, rows);
    }

    private ProductSale enrich(ProductSale row, BigDecimal totalQuantity, BigDecimal previousQuantity) {
        int sharePercent = totalQuantity.compareTo(BigDecimal.ZERO) <= 0
                ? 0
                : row.quantity().multiply(BigDecimal.valueOf(100)).divide(totalQuantity, 0, java.math.RoundingMode.HALF_UP).intValue();
        int trendPercent = ReportFilterUtils.percentChange(row.quantity(), previousQuantity);
        return new ProductSale(
                row.productId(),
                row.productName(),
                row.chartLabel(),
                row.categoryName(),
                row.quantity(),
                row.revenue(),
                sharePercent,
                trendPercent
        );
    }

    public record BestSellerReportData(ProductSummary summary, List<ProductSale> products) {
    }
}
