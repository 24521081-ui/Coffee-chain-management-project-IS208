package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.RevenuePeriodType;
import com.phungloccoffee.model.report.RevenueReportFilter;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.BranchRevenue;
import com.phungloccoffee.model.report.ReportModels.DailyRevenue;
import com.phungloccoffee.model.report.ReportModels.RevenueSummary;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RevenueReportDAO {
    private final ReportLookupDAO lookupDAO = new ReportLookupDAO();

    public List<BranchOption> findBranchOptions() throws DatabaseException {
        return lookupDAO.findBranchOptions();
    }

    public RevenueAggregate findAggregate(RevenueReportFilter filter) throws DatabaseException {
        String sql = """
                SELECT NVL(SUM(dh.tong_tien), 0) AS revenue,
                       COUNT(dh.don_hang_id) AS orders
                FROM don_hang dh
                WHERE dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR dh.chi_nhanh_id = ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndNullableBranch(stmt, filter.fromDate(), filter.toDate(), filter.branchId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RevenueAggregate(rs.getBigDecimal("revenue"), rs.getInt("orders"));
                }
                return new RevenueAggregate(BigDecimal.ZERO, 0);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải tổng quan báo cáo doanh thu.", e);
        }
    }

    public List<RevenueTrendRaw> findRevenueTrend(RevenueReportFilter filter) throws DatabaseException {
        TrendGroupSql groupSql = trendGroupSql(filter.periodType());
        String sql = """
                SELECT %s AS period_start,
                       %s AS period_label,
                       NVL(SUM(dh.tong_tien), 0) AS revenue
                FROM don_hang dh
                WHERE dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR dh.chi_nhanh_id = ?)
                GROUP BY %s, %s
                ORDER BY period_start
                """.formatted(groupSql.periodStartExpression(), groupSql.labelExpression(),
                groupSql.periodStartExpression(), groupSql.labelExpression());
        List<RevenueTrendRaw> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndNullableBranch(stmt, filter.fromDate(), filter.toDate(), filter.branchId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RevenueTrendRaw(
                            rs.getDate("period_start").toLocalDate(),
                            rs.getString("period_label"),
                            rs.getBigDecimal("revenue")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải xu hướng doanh thu.", e);
        }
    }

    public List<BranchRevenueRaw> findBranchRevenue(RevenueReportFilter filter) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       NVL(SUM(dh.tong_tien), 0) AS revenue,
                       COUNT(dh.don_hang_id) AS orders
                FROM chi_nhanh cn
                LEFT JOIN don_hang dh ON dh.chi_nhanh_id = cn.chi_nhanh_id
                  AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                WHERE cn.trang_thai = 1
                  AND (? IS NULL OR cn.chi_nhanh_id = ?)
                GROUP BY cn.chi_nhanh_id, cn.ten_chi_nhanh
                ORDER BY revenue DESC, cn.ten_chi_nhanh
                """;
        List<BranchRevenueRaw> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndNullableBranch(stmt, filter.fromDate(), filter.toDate(), filter.branchId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("ten_chi_nhanh");
                    String displayName = ReportLookupDAO.normalizeBranchName(fullName);
                    rows.add(new BranchRevenueRaw(
                            rs.getString("chi_nhanh_id"),
                            fullName,
                            displayName,
                            ReportLookupDAO.chartLabel(displayName),
                            rs.getBigDecimal("revenue"),
                            rs.getInt("orders")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải doanh thu theo chi nhánh.", e);
        }
    }

    public RevenueSummary findSummary(LocalDate fromDate, LocalDate toDate, String branchId) throws DatabaseException {
        String sql = """
                SELECT NVL(SUM(dh.tong_tien), 0) AS revenue,
                       COUNT(dh.don_hang_id) AS orders,
                       NVL(SUM(ct.total_qty), 0) AS products_sold
                FROM don_hang dh
                LEFT JOIN (
                    SELECT don_hang_id, SUM(so_luong) AS total_qty
                    FROM chi_tiet_don_hang
                    GROUP BY don_hang_id
                ) ct ON ct.don_hang_id = dh.don_hang_id
                WHERE dh.trang_thai = 'DA_HOAN_THANH'
                  AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR ? = '' OR dh.chi_nhanh_id = ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, fromDate, toDate, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RevenueSummary(
                            rs.getBigDecimal("revenue"),
                            rs.getInt("orders"),
                            rs.getBigDecimal("products_sold"),
                            0
                    );
                }
                return new RevenueSummary(BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải tổng quan doanh thu.", e);
        }
    }

    public List<DailyRevenue> findRevenueByDate(LocalDate fromDate, LocalDate toDate, String branchId) throws DatabaseException {
        String sql = """
                SELECT TRUNC(dh.created_at) AS report_date,
                       NVL(SUM(dh.tong_tien), 0) AS revenue,
                       COUNT(dh.don_hang_id) AS orders,
                       NVL(SUM(ct.total_qty), 0) AS products_sold
                FROM don_hang dh
                LEFT JOIN (
                    SELECT don_hang_id, SUM(so_luong) AS total_qty
                    FROM chi_tiet_don_hang
                    GROUP BY don_hang_id
                ) ct ON ct.don_hang_id = dh.don_hang_id
                WHERE dh.trang_thai = 'DA_HOAN_THANH'
                  AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR ? = '' OR dh.chi_nhanh_id = ?)
                GROUP BY TRUNC(dh.created_at)
                ORDER BY report_date
                """;
        List<DailyRevenue> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, fromDate, toDate, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DailyRevenue(
                            rs.getDate("report_date").toLocalDate(),
                            rs.getBigDecimal("revenue"),
                            rs.getInt("orders"),
                            rs.getBigDecimal("products_sold")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải doanh thu theo ngày.", e);
        }
    }

    public List<BranchRevenue> findRevenueByBranch(LocalDate fromDate, LocalDate toDate, String branchId) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       NVL(SUM(dh.tong_tien), 0) AS revenue,
                       COUNT(dh.don_hang_id) AS orders,
                       NVL(SUM(ct.total_qty), 0) AS products_sold
                FROM don_hang dh
                JOIN chi_nhanh cn ON cn.chi_nhanh_id = dh.chi_nhanh_id
                LEFT JOIN (
                    SELECT don_hang_id, SUM(so_luong) AS total_qty
                    FROM chi_tiet_don_hang
                    GROUP BY don_hang_id
                ) ct ON ct.don_hang_id = dh.don_hang_id
                WHERE dh.trang_thai = 'DA_HOAN_THANH'
                  AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR ? = '' OR dh.chi_nhanh_id = ?)
                GROUP BY cn.chi_nhanh_id, cn.ten_chi_nhanh
                ORDER BY revenue DESC
                """;
        List<BranchRevenue> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, fromDate, toDate, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("ten_chi_nhanh");
                    String displayName = ReportLookupDAO.normalizeBranchName(fullName);
                    rows.add(new BranchRevenue(
                            rs.getString("chi_nhanh_id"),
                            displayName,
                            ReportLookupDAO.chartLabel(displayName),
                            fullName,
                            rs.getBigDecimal("revenue"),
                            rs.getInt("orders"),
                            rs.getBigDecimal("products_sold")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải doanh thu theo chi nhánh.", e);
        }
    }

    private void bindRangeAndBranch(PreparedStatement stmt, LocalDate fromDate, LocalDate toDate, String branchId) throws SQLException {
        stmt.setTimestamp(1, Timestamp.valueOf(fromDate.atStartOfDay()));
        stmt.setTimestamp(2, Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        stmt.setString(3, branchId);
        stmt.setString(4, branchId);
        stmt.setString(5, branchId);
    }

    private void bindRangeAndNullableBranch(PreparedStatement stmt, LocalDate fromDate, LocalDate toDate, String branchId) throws SQLException {
        stmt.setTimestamp(1, Timestamp.valueOf(fromDate.atStartOfDay()));
        stmt.setTimestamp(2, Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        stmt.setString(3, branchId);
        stmt.setString(4, branchId);
    }

    private TrendGroupSql trendGroupSql(RevenuePeriodType periodType) {
        return switch (periodType == null ? RevenuePeriodType.DAY : periodType) {
            case MONTH -> new TrendGroupSql("TRUNC(dh.created_at, 'MM')", "TO_CHAR(TRUNC(dh.created_at, 'MM'), 'MM/YYYY')");
            case QUARTER -> new TrendGroupSql("TRUNC(dh.created_at, 'Q')", "'Q' || TO_CHAR(dh.created_at, 'Q/YYYY')");
            case YEAR -> new TrendGroupSql("TRUNC(dh.created_at, 'YYYY')", "TO_CHAR(TRUNC(dh.created_at, 'YYYY'), 'YYYY')");
            case DAY, CUSTOM -> new TrendGroupSql("TRUNC(dh.created_at)", "TO_CHAR(TRUNC(dh.created_at), 'DD/MM')");
        };
    }

    public record RevenueAggregate(BigDecimal revenue, int orders) {
        public RevenueAggregate {
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }

    public record RevenueTrendRaw(LocalDate periodStart, String label, BigDecimal revenue) {
        public RevenueTrendRaw {
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }

    public record BranchRevenueRaw(String branchId, String fullName, String displayName, String chartLabel,
                                   BigDecimal revenue, int orders) {
        public BranchRevenueRaw {
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }

    private record TrendGroupSql(String periodStartExpression, String labelExpression) {
    }
}
