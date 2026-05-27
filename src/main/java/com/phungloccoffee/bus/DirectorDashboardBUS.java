package com.phungloccoffee.bus;

import com.phungloccoffee.dao.DirectorDashboardDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.dashboard.BranchRevenueSummary;
import com.phungloccoffee.model.dashboard.DashboardData;
import com.phungloccoffee.model.dashboard.DashboardFilter;
import com.phungloccoffee.model.dashboard.DashboardKpi;
import com.phungloccoffee.model.dashboard.DashboardMetric;
import com.phungloccoffee.model.dashboard.DashboardSummary;
import com.phungloccoffee.model.dashboard.MetricStatus;
import com.phungloccoffee.model.dashboard.PeriodType;
import com.phungloccoffee.model.dashboard.RevenuePoint;
import com.phungloccoffee.model.dashboard.TopProductSummary;
import com.phungloccoffee.model.report.ReportModels.BranchOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DirectorDashboardBUS {
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));

    private final DirectorDashboardDAO dashboardDAO = new DirectorDashboardDAO();

    public List<BranchOption> getBranchOptions() throws DatabaseException {
        return dashboardDAO.findBranchOptions();
    }

    public DashboardData getDashboardData(DashboardFilter filter) throws DatabaseException {
        validateFilter(filter);

        DashboardSummary current = dashboardDAO.findSummary(filter);
        DashboardSummary previous = dashboardDAO.findSummary(previousFilter(filter));
        List<RevenuePoint> revenueByDate = fillMissingRevenueDays(filter, dashboardDAO.findRevenueByDate(filter));
        Optional<TopProductSummary> topProduct = dashboardDAO.findTopProduct(filter);
        Optional<BranchRevenueSummary> branchSummary = filter.allBranches()
                ? dashboardDAO.findTopBranch(filter)
                : dashboardDAO.findBranchById(filter.branchId());

        DashboardKpi revenueKpi = moneyKpi(current.revenue(), previous.revenue());
        DashboardKpi orderKpi = countKpi(BigDecimal.valueOf(current.orderCount()), BigDecimal.valueOf(previous.orderCount()), current.orderCount() == 0);
        DashboardKpi productKpi = countKpi(current.productsSold(), previous.productsSold(), current.productsSold().compareTo(BigDecimal.ZERO) == 0);
        DashboardKpi inventoryKpi = inventoryKpi(current.inventoryWarnings());
        String branchName = branchSummary.map(BranchRevenueSummary::branchName)
                .filter(name -> !name.isBlank())
                .orElse(filter.allBranches() ? "Tất cả chi nhánh" : filter.branchId());
        String branchOverviewLabel = filter.allBranches() ? "Chi nhánh dẫn đầu" : "Chi nhánh đang xem";

        return new DashboardData(
                filter,
                revenueKpi,
                orderKpi,
                productKpi,
                inventoryKpi,
                revenueByDate,
                formatPeriod(filter),
                branchOverviewLabel,
                branchSummary.orElse(null),
                topProduct.orElse(null),
                buildActivityMetrics(branchName, current, revenueKpi, orderKpi, productKpi, inventoryKpi, topProduct.orElse(null))
        );
    }

    public DashboardFilter resolvePeriod(DashboardFilter filter, Integer selectedMonth, Integer selectedYear) {
        PeriodType periodType = filter.periodType();
        LocalDate today = LocalDate.now();
        int year = selectedYear == null ? today.getYear() : selectedYear;
        int month = selectedMonth == null ? today.getMonthValue() : selectedMonth;

        return switch (periodType) {
            case TODAY -> new DashboardFilter(today, today, filter.branchId(), periodType);
            case THIS_WEEK -> new DashboardFilter(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                    filter.branchId(),
                    periodType
            );
            case THIS_MONTH -> {
                LocalDate firstDay = LocalDate.of(year, month, 1);
                yield new DashboardFilter(firstDay, firstDay.withDayOfMonth(firstDay.lengthOfMonth()), filter.branchId(), periodType);
            }
            case THIS_YEAR -> new DashboardFilter(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), filter.branchId(), periodType);
            case CUSTOM -> filter;
        };
    }

    private void validateFilter(DashboardFilter filter) {
        if (filter == null || filter.fromDate() == null || filter.toDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ khoảng thời gian báo cáo");
        }
        if (filter.fromDate().isAfter(filter.toDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
    }

    private DashboardFilter previousFilter(DashboardFilter filter) {
        long days = ChronoUnit.DAYS.between(filter.fromDate(), filter.toDate()) + 1;
        LocalDate previousTo = filter.fromDate().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        return new DashboardFilter(previousFrom, previousTo, filter.branchId(), filter.periodType());
    }

    private List<RevenuePoint> fillMissingRevenueDays(DashboardFilter filter, List<RevenuePoint> points) {
        Map<LocalDate, BigDecimal> revenueByDate = points.stream()
                .collect(Collectors.toMap(RevenuePoint::date, RevenuePoint::revenue, BigDecimal::add, LinkedHashMap::new));
        List<RevenuePoint> filled = new ArrayList<>();
        LocalDate date = filter.fromDate();
        while (!date.isAfter(filter.toDate())) {
            filled.add(new RevenuePoint(date, revenueByDate.getOrDefault(date, BigDecimal.ZERO)));
            date = date.plusDays(1);
        }
        return filled;
    }

    private DashboardKpi moneyKpi(BigDecimal current, BigDecimal previous) {
        String change = formatChange(current, previous);
        MetricStatus status = statusFromDecrease(current, previous, current.compareTo(BigDecimal.ZERO) == 0);
        return new DashboardKpi(current, change, status, noteFromChange(change, status));
    }

    private DashboardKpi countKpi(BigDecimal current, BigDecimal previous, boolean forceDangerWhenZero) {
        String change = formatChange(current, previous);
        MetricStatus status = statusFromDecrease(current, previous, forceDangerWhenZero);
        return new DashboardKpi(current, change, status, noteFromChange(change, status));
    }

    private DashboardKpi inventoryKpi(int warningCount) {
        MetricStatus status = switch (warningCount) {
            case 0 -> MetricStatus.GOOD;
            case 1, 2 -> MetricStatus.WARNING;
            default -> MetricStatus.DANGER;
        };
        String note = warningCount == 0 ? "Không có mặt hàng dưới mức tồn tối thiểu" : "Cần kiểm tra tồn kho hiện tại";
        return new DashboardKpi(BigDecimal.valueOf(warningCount), "", status, note);
    }

    private MetricStatus statusFromDecrease(BigDecimal current, BigDecimal previous, boolean forceDangerWhenZero) {
        if (forceDangerWhenZero && current.compareTo(BigDecimal.ZERO) == 0) {
            return MetricStatus.DANGER;
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? MetricStatus.NEW : MetricStatus.DANGER;
        }
        BigDecimal change = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
        if (change.compareTo(BigDecimal.valueOf(-30)) <= 0) {
            return MetricStatus.DANGER;
        }
        if (change.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            return MetricStatus.WARNING;
        }
        return MetricStatus.GOOD;
    }

    private String formatChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "Mới" : "0%";
        }
        BigDecimal change = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP);
        return (change.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + change.toPlainString() + "%";
    }

    private String noteFromChange(String change, MetricStatus status) {
        return switch (status) {
            case NEW -> "Phát sinh dữ liệu mới so với kỳ trước";
            case GOOD -> "Biến động " + change + " so với kỳ trước";
            case WARNING -> "Giảm " + change.replace("-", "") + " so với kỳ trước";
            case DANGER -> "Cần kiểm tra vì chỉ số thấp hơn kỳ trước";
        };
    }

    private List<DashboardMetric> buildActivityMetrics(String branchName, DashboardSummary summary,
                                                       DashboardKpi revenueKpi, DashboardKpi orderKpi,
                                                       DashboardKpi productKpi, DashboardKpi inventoryKpi,
                                                       TopProductSummary topProduct) {
        return List.of(
                new DashboardMetric(branchName, "Doanh thu", formatMoney(summary.revenue()), revenueKpi.status(), revenueKpi.note()),
                new DashboardMetric(branchName, "Đơn hàng", NUMBER_FORMATTER.format(summary.orderCount()), orderKpi.status(), orderKpi.note()),
                new DashboardMetric(branchName, "Sản phẩm bán ra", formatNumber(summary.productsSold()), productKpi.status(),
                        topProduct == null ? "Không có dữ liệu sản phẩm bán chạy" : "Top: " + topProduct.productName()),
                new DashboardMetric(branchName, "Cảnh báo tồn kho", NUMBER_FORMATTER.format(summary.inventoryWarnings()),
                        inventoryKpi.status(), inventoryKpi.note())
        );
    }

    private String formatPeriod(DashboardFilter filter) {
        return filter.fromDate().format(PERIOD_FORMATTER) + " - " + filter.toDate().format(PERIOD_FORMATTER);
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal million = amount.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.HALF_UP);
        if (million.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return million.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "B";
        }
        return million.stripTrailingZeros().toPlainString() + "M";
    }

    private String formatNumber(BigDecimal value) {
        return NUMBER_FORMATTER.format(value.setScale(0, RoundingMode.HALF_UP));
    }
}
