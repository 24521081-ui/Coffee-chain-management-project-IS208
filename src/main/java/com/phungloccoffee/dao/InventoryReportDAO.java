package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.ReportModels.InventoryItem;
import com.phungloccoffee.util.DBConnection;

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
}
