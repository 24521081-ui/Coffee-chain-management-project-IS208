package com.phungloccoffee.gui.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class ReportFilterUtils {
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final NumberFormat INTEGER_FORMATTER = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));

    private ReportFilterUtils() {
    }

    public static LocalDate defaultFromDate() {
        return LocalDate.now().minusDays(6);
    }

    public static LocalDate defaultToDate() {
        return LocalDate.now();
    }

    public static String validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return "Vui lòng chọn đầy đủ khoảng thời gian báo cáo";
        }
        if (fromDate.isAfter(toDate)) {
            return "Ngày bắt đầu không được lớn hơn ngày kết thúc";
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > 31) {
            return "Vui lòng chọn khoảng thời gian không quá 31 ngày để báo cáo hiển thị ổn định";
        }
        return null;
    }

    public static String formatDateLabel(LocalDate date) {
        return date == null ? "" : date.format(DATE_LABEL_FORMATTER);
    }

    public static String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal million = safeAmount.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.HALF_UP);
        if (million.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            BigDecimal billion = million.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP);
            return stripTrailingZero(billion) + "B";
        }
        return stripTrailingZero(million) + "M";
    }

    public static String formatNumber(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return INTEGER_FORMATTER.format(safeValue.setScale(0, RoundingMode.HALF_UP));
    }

    public static String formatNumber(int value) {
        return INTEGER_FORMATTER.format(value);
    }

    public static String formatPercent(int value) {
        return (value > 0 ? "+" : "") + value + "%";
    }

    public static String averageOrderValue(BigDecimal revenue, int orders) {
        if (orders <= 0) {
            return "0K";
        }
        BigDecimal averageThousand = (revenue == null ? BigDecimal.ZERO : revenue)
                .divide(BigDecimal.valueOf(orders * 1000L), 0, RoundingMode.HALF_UP);
        return INTEGER_FORMATTER.format(averageThousand) + "K";
    }

    public static int percentChange(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;
        if (safePrevious.compareTo(BigDecimal.ZERO) <= 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private static String stripTrailingZero(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
