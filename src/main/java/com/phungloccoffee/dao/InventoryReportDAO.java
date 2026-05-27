package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.InventoryCategoryOption;
import com.phungloccoffee.model.report.InventoryReportFilter;
import com.phungloccoffee.model.report.InventoryStatus;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.InventoryItem;
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

import static com.phungloccoffee.model.report.ReportModels.ALL_CATEGORY;
import static com.phungloccoffee.model.report.ReportModels.ALL_STATUS;

public class InventoryReportDAO {
    private static final String BASE_INVENTORY_FROM = """
            FROM ton_kho tk
            JOIN kho k ON k.kho_id = tk.kho_id
            JOIN chi_nhanh cn ON cn.chi_nhanh_id = k.chi_nhanh_id
            JOIN san_pham sp ON sp.san_pham_id = tk.san_pham_id
            LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
            WHERE tk.last_updated >= ?
              AND tk.last_updated < ?
              AND (? IS NULL OR cn.chi_nhanh_id = ?)
              AND (? IS NULL OR sp.danh_muc_id = ?)
              AND (
                  ? IS NULL
                  OR (? = 'OUT_OF_STOCK' AND tk.so_luong_ton <= 0)
                  OR (? = 'LOW_STOCK' AND tk.so_luong_ton > 0 AND tk.so_luong_ton <= tk.muc_ton_toi_thieu)
                  OR (? = 'STABLE' AND tk.so_luong_ton > tk.muc_ton_toi_thieu)
              )
            """;

    public List<BranchOption> findBranchOptions() throws DatabaseException {
        return new ReportLookupDAO().findBranchOptions();
    }

    public List<InventoryCategoryOption> findCategoryOptions() throws DatabaseException {
        String sql = """
                SELECT danh_muc_id, ten_danh_muc
                FROM danh_muc_san_pham
                ORDER BY ten_danh_muc
                """;
        List<InventoryCategoryOption> categories = new ArrayList<>();
        categories.add(new InventoryCategoryOption(null, "Tất cả nhóm"));
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(new InventoryCategoryOption(rs.getString("danh_muc_id"), rs.getString("ten_danh_muc")));
            }
            return categories;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải nhóm nguyên liệu.", e);
        }
    }

    public LocalDate findLatestInventoryDate() throws DatabaseException {
        String sql = "SELECT MAX(last_updated) AS latest_date FROM ton_kho";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getTimestamp("latest_date") != null) {
                return rs.getTimestamp("latest_date").toLocalDateTime().toLocalDate();
            }
            return LocalDate.now();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải ngày dữ liệu tồn kho mới nhất.", e);
        }
    }

    public List<InventoryItemRaw> findInventoryItems(InventoryReportFilter filter) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       sp.san_pham_id,
                       sp.ten_san_pham,
                       sp.danh_muc_id,
                       NVL(dm.ten_danh_muc, sp.loai_san_pham) AS category_name,
                       tk.so_luong_ton,
                       tk.muc_ton_toi_thieu,
                       sp.gia_von,
                       NVL(tk.so_luong_ton * sp.gia_von, 0) AS inventory_value
                """ + BASE_INVENTORY_FROM + """
                ORDER BY cn.ten_chi_nhanh, category_name, sp.ten_san_pham
                """;
        List<InventoryItemRaw> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFilter(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullBranchName = rs.getString("ten_chi_nhanh");
                    rows.add(new InventoryItemRaw(
                            rs.getString("san_pham_id"),
                            rs.getString("ten_san_pham"),
                            rs.getString("chi_nhanh_id"),
                            ReportLookupDAO.normalizeBranchName(fullBranchName),
                            rs.getString("danh_muc_id"),
                            rs.getString("category_name"),
                            rs.getBigDecimal("so_luong_ton"),
                            rs.getBigDecimal("muc_ton_toi_thieu"),
                            rs.getBigDecimal("gia_von"),
                            rs.getBigDecimal("inventory_value")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải bảng tồn kho nguyên liệu.", e);
        }
    }

    public List<InventoryBranchAlertRaw> findBranchAlerts(InventoryReportFilter filter) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       NVL(SUM(CASE WHEN tk.so_luong_ton <= tk.muc_ton_toi_thieu THEN 1 ELSE 0 END), 0) AS alert_count
                FROM chi_nhanh cn
                LEFT JOIN kho k ON k.chi_nhanh_id = cn.chi_nhanh_id
                LEFT JOIN ton_kho tk ON tk.kho_id = k.kho_id
                  AND tk.last_updated >= ?
                  AND tk.last_updated < ?
                LEFT JOIN san_pham sp ON sp.san_pham_id = tk.san_pham_id
                WHERE cn.trang_thai = 1
                  AND (? IS NULL OR cn.chi_nhanh_id = ?)
                  AND (? IS NULL OR sp.danh_muc_id = ?)
                  AND (
                      ? IS NULL
                      OR (? = 'OUT_OF_STOCK' AND tk.so_luong_ton <= 0)
                      OR (? = 'LOW_STOCK' AND tk.so_luong_ton > 0 AND tk.so_luong_ton <= tk.muc_ton_toi_thieu)
                      OR (? = 'STABLE' AND tk.so_luong_ton > tk.muc_ton_toi_thieu)
                  )
                GROUP BY cn.chi_nhanh_id, cn.ten_chi_nhanh
                ORDER BY alert_count DESC, cn.ten_chi_nhanh
                """;
        List<InventoryBranchAlertRaw> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFilter(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String displayName = ReportLookupDAO.normalizeBranchName(rs.getString("ten_chi_nhanh"));
                    rows.add(new InventoryBranchAlertRaw(
                            rs.getString("chi_nhanh_id"),
                            displayName,
                            ReportLookupDAO.chartLabel(displayName),
                            rs.getInt("alert_count")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải cảnh báo tồn kho theo chi nhánh.", e);
        }
    }

    public List<InventoryCategoryValueRaw> findCategoryValues(InventoryReportFilter filter) throws DatabaseException {
        String sql = """
                SELECT NVL(sp.danh_muc_id, 'NO_CATEGORY') AS category_id,
                       NVL(dm.ten_danh_muc, sp.loai_san_pham) AS category_name,
                       NVL(SUM(tk.so_luong_ton * sp.gia_von), 0) AS inventory_value
                """ + BASE_INVENTORY_FROM + """
                GROUP BY NVL(sp.danh_muc_id, 'NO_CATEGORY'), NVL(dm.ten_danh_muc, sp.loai_san_pham)
                ORDER BY inventory_value DESC, category_name
                """;
        List<InventoryCategoryValueRaw> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindFilter(stmt, filter);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new InventoryCategoryValueRaw(
                            rs.getString("category_id"),
                            rs.getString("category_name"),
                            rs.getBigDecimal("inventory_value")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải cơ cấu tồn kho theo nhóm.", e);
        }
    }

    private void bindFilter(PreparedStatement stmt, InventoryReportFilter filter) throws SQLException {
        stmt.setTimestamp(1, Timestamp.valueOf(filter.fromDate().atStartOfDay()));
        stmt.setTimestamp(2, Timestamp.valueOf(filter.toDate().plusDays(1).atStartOfDay()));
        stmt.setString(3, filter.branchId());
        stmt.setString(4, filter.branchId());
        stmt.setString(5, filter.categoryId());
        stmt.setString(6, filter.categoryId());
        String statusName = filter.status() == null ? null : filter.status().name();
        stmt.setString(7, statusName);
        stmt.setString(8, statusName);
        stmt.setString(9, statusName);
        stmt.setString(10, statusName);
    }

    public List<InventoryItem> findInventoryItems(LocalDate fromDate, LocalDate toDate, String branchId,
                                                  String categoryName, String status) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       sp.san_pham_id,
                       sp.ten_san_pham,
                       NVL(dm.ten_danh_muc, sp.loai_san_pham) AS category_name,
                       tk.so_luong_ton,
                       tk.muc_ton_toi_thieu,
                       NVL(tk.so_luong_ton * sp.gia_von, 0) AS inventory_value,
                       CASE
                           WHEN tk.so_luong_ton <= 0 THEN 'Hết hàng'
                           WHEN tk.so_luong_ton < tk.muc_ton_toi_thieu THEN 'Tồn thấp'
                           ELSE 'Ổn định'
                       END AS stock_status
                FROM ton_kho tk
                JOIN kho k ON k.kho_id = tk.kho_id
                JOIN chi_nhanh cn ON cn.chi_nhanh_id = k.chi_nhanh_id
                JOIN san_pham sp ON sp.san_pham_id = tk.san_pham_id
                LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                WHERE tk.last_updated < ?
                  AND (? IS NULL OR ? = '' OR cn.chi_nhanh_id = ?)
                  AND (? IS NULL OR ? = ? OR dm.ten_danh_muc = ?)
                  AND (
                      ? IS NULL OR ? = ?
                      OR (? = 'Hết hàng' AND tk.so_luong_ton <= 0)
                      OR (? = 'Tồn thấp' AND tk.so_luong_ton > 0 AND tk.so_luong_ton < tk.muc_ton_toi_thieu)
                      OR (? = 'Ổn định' AND tk.so_luong_ton >= tk.muc_ton_toi_thieu)
                  )
                ORDER BY stock_status, cn.ten_chi_nhanh, sp.ten_san_pham
                """;
        List<InventoryItem> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            stmt.setString(2, branchId);
            stmt.setString(3, branchId);
            stmt.setString(4, branchId);
            stmt.setString(5, categoryName);
            stmt.setString(6, categoryName);
            stmt.setString(7, ALL_CATEGORY);
            stmt.setString(8, categoryName);
            stmt.setString(9, status);
            stmt.setString(10, status);
            stmt.setString(11, ALL_STATUS);
            stmt.setString(12, status);
            stmt.setString(13, status);
            stmt.setString(14, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String branchName = ReportLookupDAO.normalizeBranchName(rs.getString("ten_chi_nhanh"));
                    rows.add(new InventoryItem(
                            rs.getString("chi_nhanh_id"),
                            branchName,
                            rs.getString("san_pham_id"),
                            rs.getString("ten_san_pham"),
                            rs.getString("category_name"),
                            rs.getBigDecimal("so_luong_ton"),
                            rs.getBigDecimal("muc_ton_toi_thieu"),
                            rs.getBigDecimal("inventory_value"),
                            rs.getString("stock_status")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải báo cáo tồn kho.", e);
        }
    }

    public record InventoryItemRaw(String productId, String productName, String branchId, String branchName,
                                   String categoryId, String categoryName, BigDecimal quantityOnHand,
                                   BigDecimal minQuantity, BigDecimal costPrice, BigDecimal inventoryValue) {
        public InventoryItemRaw {
            quantityOnHand = quantityOnHand == null ? BigDecimal.ZERO : quantityOnHand;
            minQuantity = minQuantity == null ? BigDecimal.ZERO : minQuantity;
            costPrice = costPrice == null ? BigDecimal.ZERO : costPrice;
            inventoryValue = inventoryValue == null ? BigDecimal.ZERO : inventoryValue;
        }
    }

    public record InventoryBranchAlertRaw(String branchId, String branchName, String chartLabel, int alertCount) {
    }

    public record InventoryCategoryValueRaw(String categoryId, String categoryName, BigDecimal inventoryValue) {
        public InventoryCategoryValueRaw {
            inventoryValue = inventoryValue == null ? BigDecimal.ZERO : inventoryValue;
        }
    }
}
