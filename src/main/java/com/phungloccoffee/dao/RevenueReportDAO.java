package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
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
}
