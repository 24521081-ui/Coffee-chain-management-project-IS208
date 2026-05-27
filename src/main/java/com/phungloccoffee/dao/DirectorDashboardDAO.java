package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.dashboard.BranchRevenueSummary;
import com.phungloccoffee.model.dashboard.DashboardFilter;
import com.phungloccoffee.model.dashboard.DashboardSummary;
import com.phungloccoffee.model.dashboard.RevenuePoint;
import com.phungloccoffee.model.dashboard.TopProductSummary;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DirectorDashboardDAO {
    private final ReportLookupDAO lookupDAO = new ReportLookupDAO();

    public List<BranchOption> findBranchOptions() throws DatabaseException {
        return lookupDAO.findBranchOptions();
    }

    public DashboardSummary findSummary(DashboardFilter filter) throws DatabaseException {
        String sql = """
                SELECT NVL(SUM(dh.tong_tien), 0) AS revenue,
                       COUNT(dh.don_hang_id) AS order_count,
                       NVL(SUM(ct.total_qty), 0) AS products_sold
                FROM don_hang dh
                LEFT JOIN (
                    SELECT don_hang_id, SUM(so_luong) AS total_qty
                    FROM chi_tiet_don_hang
                    GROUP BY don_hang_id
                ) ct ON ct.don_hang_id = dh.don_hang_id
                WHERE dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR dh.chi_nhanh_id = ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DashboardSummary(
                            rs.getBigDecimal("revenue"),
                            rs.getInt("order_count"),
                            rs.getBigDecimal("products_sold"),
                            findInventoryWarningCount(filter)
                    );
                }
            }
            return new DashboardSummary(BigDecimal.ZERO, 0, BigDecimal.ZERO, findInventoryWarningCount(filter));
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải tổng quan DirectorDashboard.", e);
        }
    }

    public int findInventoryWarningCount(DashboardFilter filter) throws DatabaseException {
        String sql = """
                SELECT COUNT(*) AS warning_count
                FROM ton_kho tk
                JOIN kho k ON k.kho_id = tk.kho_id
                WHERE tk.so_luong_ton <= tk.muc_ton_toi_thieu
                  AND (? IS NULL OR k.chi_nhanh_id = ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filter.branchId());
            stmt.setString(2, filter.branchId());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("warning_count") : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải cảnh báo tồn kho DirectorDashboard.", e);
        }
    }

    public List<RevenuePoint> findRevenueByDate(DashboardFilter filter) throws DatabaseException {
        String sql = """
                SELECT TRUNC(dh.created_at) AS revenue_date,
                       NVL(SUM(dh.tong_tien), 0) AS revenue
                FROM don_hang dh
                WHERE dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR dh.chi_nhanh_id = ?)
                GROUP BY TRUNC(dh.created_at)
                ORDER BY revenue_date
                """;
        List<RevenuePoint> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RevenuePoint(rs.getDate("revenue_date").toLocalDate(), rs.getBigDecimal("revenue")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải doanh thu theo ngày DirectorDashboard.", e);
        }
    }

    public Optional<BranchRevenueSummary> findTopBranch(DashboardFilter filter) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       NVL(SUM(dh.tong_tien), 0) AS revenue
                FROM don_hang dh
                JOIN chi_nhanh cn ON cn.chi_nhanh_id = dh.chi_nhanh_id
                WHERE dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR dh.chi_nhanh_id = ?)
                GROUP BY cn.chi_nhanh_id, cn.ten_chi_nhanh
                ORDER BY revenue DESC
                FETCH FIRST 1 ROWS ONLY
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new BranchRevenueSummary(
                            rs.getString("chi_nhanh_id"),
                            ReportLookupDAO.normalizeBranchName(rs.getString("ten_chi_nhanh")),
                            rs.getBigDecimal("revenue")
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải chi nhánh dẫn đầu DirectorDashboard.", e);
        }
    }

    public Optional<BranchRevenueSummary> findBranchById(String branchId) throws DatabaseException {
        String sql = """
                SELECT chi_nhanh_id, ten_chi_nhanh
                FROM chi_nhanh
                WHERE chi_nhanh_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new BranchRevenueSummary(
                            rs.getString("chi_nhanh_id"),
                            ReportLookupDAO.normalizeBranchName(rs.getString("ten_chi_nhanh")),
                            BigDecimal.ZERO
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải thông tin chi nhánh DirectorDashboard.", e);
        }
    }

    public Optional<TopProductSummary> findTopProduct(DashboardFilter filter) throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id,
                       sp.ten_san_pham,
                       NVL(SUM(ct.so_luong), 0) AS quantity
                FROM don_hang dh
                JOIN chi_tiet_don_hang ct ON ct.don_hang_id = dh.don_hang_id
                JOIN san_pham sp ON sp.san_pham_id = ct.san_pham_id
                WHERE dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.trang_thai <> 'DA_HUY'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR dh.chi_nhanh_id = ?)
                GROUP BY sp.san_pham_id, sp.ten_san_pham
                ORDER BY quantity DESC
                FETCH FIRST 1 ROWS ONLY
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRangeAndBranch(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TopProductSummary(
                            rs.getString("san_pham_id"),
                            rs.getString("ten_san_pham"),
                            rs.getBigDecimal("quantity")
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải sản phẩm bán chạy DirectorDashboard.", e);
        }
    }

    private void bindRangeAndBranch(PreparedStatement stmt, DashboardFilter filter) throws SQLException {
        stmt.setTimestamp(1, Timestamp.valueOf(filter.fromDate().atStartOfDay()));
        stmt.setTimestamp(2, Timestamp.valueOf(filter.toDate().plusDays(1).atStartOfDay()));
        stmt.setString(3, filter.branchId());
        stmt.setString(4, filter.branchId());
    }
}
