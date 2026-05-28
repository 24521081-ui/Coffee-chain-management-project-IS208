package com.phungloccoffee.bus;

import com.phungloccoffee.dao.RevenueReportDAO;
import com.phungloccoffee.dao.RevenueReportDAO.BranchRevenueRaw;
import com.phungloccoffee.dao.RevenueReportDAO.RevenueAggregate;
import com.phungloccoffee.dao.RevenueReportDAO.RevenueTrendRaw;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.BranchRevenueReport;
import com.phungloccoffee.model.report.BranchRevenueStatus;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.RevenuePeriodType;
import com.phungloccoffee.model.report.RevenueReportData;
import com.phungloccoffee.model.report.RevenueReportFilter;
import com.phungloccoffee.model.report.RevenueSummary;
import com.phungloccoffee.model.report.RevenueTrendPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RevenueReportBUS {
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter YEAR_LABEL = DateTimeFormatter.ofPattern("yyyy");

    private final RevenueReportDAO revenueReportDAO = new RevenueReportDAO();

    public List<BranchOption> getBranchOptions() throws DatabaseException {
        return revenueReportDAO.findBranchOptions();
    }

    public RevenueReportData getRevenueReport(RevenueReportFilter filter) throws DatabaseException {
        validateFilter(filter);

        RevenueReportFilter previousFilter = previousFilter(filter);
        RevenueAggregate current = revenueReportDAO.findAggregate(filter);
        RevenueAggregate previous = revenueReportDAO.findAggregate(previousFilter);
        RevenueSummary summary = buildSummary(current, previous);

        List<RevenueTrendPoint> trendPoints = fillMissingTrendPoints(filter, revenueReportDAO.findRevenueTrend(filter));
        List<BranchRevenueRaw> currentBranches = revenueReportDAO.findBranchRevenue(filter);
        Map<String, BranchRevenueRaw> previousBranches = revenueReportDAO.findBranchRevenue(previousFilter).stream()
                .collect(Collectors.toMap(BranchRevenueRaw::branchId, Function.identity()));
        List<BranchRevenueReport> branchReports = currentBranches.stream()
                .map(branch -> buildBranchReport(branch, previousBranches.get(branch.branchId())))
                .toList();
        List<BranchRevenueReport> topBranches = branchReports.stream()
                .sorted((left, right) -> right.revenue().compareTo(left.revenue()))
                .limit(filter.allBranches() ? 5 : 1)
                .toList();

        return new RevenueReportData(
                summary,
                trendPoints,
                branchReports,
                topBranches,
                filter.allBranches() ? "Xu hướng doanh thu toàn hệ thống" : "Xu hướng doanh thu chi nhánh",
                "Theo " + filter.periodType().displayName().toLowerCase() + " trong kỳ đã chọn",
                filter.allBranches() ? "Top chi nhánh doanh thu" : "Doanh thu chi nhánh đang chọn",
                filter.allBranches() ? "So sánh doanh thu theo chi nhánh" : "Dữ liệu của chi nhánh đang chọn"
        );
    }

    private void validateFilter(RevenueReportFilter filter) {
        if (filter == null || filter.fromDate() == null || filter.toDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ khoảng thời gian báo cáo");
        }
        if (filter.fromDate().isAfter(filter.toDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
    }

    private RevenueReportFilter previousFilter(RevenueReportFilter filter) {
        long days = ChronoUnit.DAYS.between(filter.fromDate(), filter.toDate()) + 1;
        LocalDate previousTo = filter.fromDate().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        return new RevenueReportFilter(filter.periodType(), previousFrom, previousTo, filter.branchId());
    }

    private RevenueSummary buildSummary(RevenueAggregate current, RevenueAggregate previous) {
        BigDecimal average = averageOrderValue(current.revenue(), current.orders());
        BigDecimal growth = growthPercent(current.revenue(), previous.revenue());
        return new RevenueSummary(current.revenue(), current.orders(), average, growth, growthLabel(current.revenue(), previous.revenue()));
    }

    private BranchRevenueReport buildBranchReport(BranchRevenueRaw current, BranchRevenueRaw previous) {
        BigDecimal previousRevenue = previous == null ? BigDecimal.ZERO : previous.revenue();
        BigDecimal growth = growthPercent(current.revenue(), previousRevenue);
        return new BranchRevenueReport(
                current.branchId(),
                current.fullName(),
                current.chartLabel(),
                current.revenue(),
                current.orders(),
                averageOrderValue(current.revenue(), current.orders()),
                growth,
                growthLabel(current.revenue(), previousRevenue),
                statusFromGrowth(growth)
        );
    }

    private List<RevenueTrendPoint> fillMissingTrendPoints(RevenueReportFilter filter, List<RevenueTrendRaw> rawPoints) {
        Map<LocalDate, RevenueTrendRaw> rawByStart = rawPoints.stream()
                .collect(Collectors.toMap(RevenueTrendRaw::periodStart, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<RevenueTrendPoint> points = new ArrayList<>();
        LocalDate cursor = normalizePeriodStart(filter.fromDate(), filter.periodType());
        LocalDate end = normalizePeriodStart(filter.toDate(), filter.periodType());
        while (!cursor.isAfter(end)) {
            RevenueTrendRaw raw = rawByStart.get(cursor);
            points.add(new RevenueTrendPoint(labelFor(cursor, filter.periodType()), cursor, raw == null ? BigDecimal.ZERO : raw.revenue()));
            cursor = nextPeriod(cursor, filter.periodType());
        }
        return points;
    }

    private LocalDate normalizePeriodStart(LocalDate date, RevenuePeriodType periodType) {
        return switch (periodType) {
            case MONTH -> date.withDayOfMonth(1);
            case QUARTER -> {
                int quarterStartMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
                yield LocalDate.of(date.getYear(), quarterStartMonth, 1);
            }
            case YEAR -> LocalDate.of(date.getYear(), 1, 1);
            case DAY, CUSTOM -> date;
        };
    }

    private LocalDate nextPeriod(LocalDate date, RevenuePeriodType periodType) {
        return switch (periodType) {
            case MONTH -> date.plusMonths(1);
            case QUARTER -> date.plusMonths(3);
            case YEAR -> date.plusYears(1);
            case DAY, CUSTOM -> date.plusDays(1);
        };
    }

    private String labelFor(LocalDate date, RevenuePeriodType periodType) {
        return switch (periodType) {
            case MONTH -> date.format(MONTH_LABEL);
            case QUARTER -> "Q" + (((date.getMonthValue() - 1) / 3) + 1) + "/" + date.getYear();
            case YEAR -> date.format(YEAR_LABEL);
            case DAY, CUSTOM -> date.format(DAY_LABEL);
        };
    }

    private BigDecimal averageOrderValue(BigDecimal revenue, int orders) {
        if (orders <= 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
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

    private BranchRevenueStatus statusFromGrowth(BigDecimal growth) {
        if (growth.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return BranchRevenueStatus.TOT;
        }
        if (growth.compareTo(BigDecimal.ZERO) >= 0) {
            return BranchRevenueStatus.ON_DINH;
        }
        if (growth.compareTo(BigDecimal.valueOf(-10)) >= 0) {
            return BranchRevenueStatus.CANH_BAO;
        }
        if (growth.compareTo(BigDecimal.valueOf(-30)) <= 0) {
            return BranchRevenueStatus.NGUY_HIEM;
        }
        return BranchRevenueStatus.KEM;
    }
}
