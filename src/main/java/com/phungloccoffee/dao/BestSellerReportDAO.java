package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.ReportModels.ProductSale;
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

public class BestSellerReportDAO {
    public List<ProductSale> findTopProducts(LocalDate fromDate, LocalDate toDate, String branchId,
                                             String categoryName, int limit) throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id,
                       sp.ten_san_pham,
                       NVL(dm.ten_danh_muc, sp.loai_san_pham) AS category_name,
                       SUM(ct.so_luong) AS quantity,
                       SUM(ct.thanh_tien) AS revenue
                FROM don_hang dh
                JOIN chi_tiet_don_hang ct ON ct.don_hang_id = dh.don_hang_id
                JOIN san_pham sp ON sp.san_pham_id = ct.san_pham_id
                LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                WHERE dh.trang_thai = 'DA_HOAN_THANH'
                  AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                  AND dh.created_at >= ?
                  AND dh.created_at < ?
                  AND (? IS NULL OR ? = '' OR dh.chi_nhanh_id = ?)
                  AND (? IS NULL OR ? = ? OR dm.ten_danh_muc = ?)
                GROUP BY sp.san_pham_id, sp.ten_san_pham, NVL(dm.ten_danh_muc, sp.loai_san_pham)
                ORDER BY quantity DESC, revenue DESC
                FETCH FIRST ? ROWS ONLY
                """;
        List<ProductSale> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(fromDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            stmt.setString(3, branchId);
            stmt.setString(4, branchId);
            stmt.setString(5, branchId);
            stmt.setString(6, categoryName);
            stmt.setString(7, categoryName);
            stmt.setString(8, ALL_CATEGORY);
            stmt.setString(9, categoryName);
            stmt.setInt(10, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String productName = rs.getString("ten_san_pham");
                    rows.add(new ProductSale(
                            rs.getString("san_pham_id"),
                            productName,
                            ReportLookupDAO.chartLabel(productName),
                            rs.getString("category_name"),
                            rs.getBigDecimal("quantity"),
                            rs.getBigDecimal("revenue"),
                            0,
                            0
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải mặt hàng bán chạy.", e);
        }
    }
}
