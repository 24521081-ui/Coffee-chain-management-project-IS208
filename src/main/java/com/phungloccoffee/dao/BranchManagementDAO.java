package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.branch.BranchManagementModels.BranchData;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BranchManagementDAO {
    public List<BranchData> findBranches(LocalDate startDate, LocalDate endDate) throws DatabaseException {
        String sql = """
                SELECT cn.chi_nhanh_id,
                       cn.ten_chi_nhanh,
                       cn.dia_chi,
                       cn.phone,
                       cn.trang_thai,
                       cn.created_at,
                       cn.updated_at,
                       mgr.ho_ten AS manager_name,
                       mgr.phone AS manager_phone,
                       mgr.email AS manager_email,
                       NVL(emp.employee_count, 0) AS employee_count,
                       NVL(prod.serving_product_count, 0) AS serving_product_count,
                       NVL(ord.order_count, 0) AS order_count,
                       NVL(ord.revenue, 0) AS revenue,
                       NVL(inv.ingredient_count, 0) AS ingredient_count
                FROM chi_nhanh cn
                LEFT JOIN (
                    SELECT chi_nhanh_id, ho_ten, phone, email
                    FROM (
                        SELECT nv.chi_nhanh_id,
                               nv.ho_ten,
                               nv.phone,
                               nv.email,
                               ROW_NUMBER() OVER (
                                   PARTITION BY nv.chi_nhanh_id
                                   ORDER BY nv.trang_thai DESC, nv.updated_at DESC
                               ) AS rn
                        FROM nhan_vien nv
                        WHERE nv.chuc_vu = 'QUAN_LY_CHI_NHANH'
                    )
                    WHERE rn = 1
                ) mgr ON mgr.chi_nhanh_id = cn.chi_nhanh_id
                LEFT JOIN (
                    SELECT chi_nhanh_id, COUNT(*) AS employee_count
                    FROM nhan_vien
                    WHERE trang_thai = 1
                    GROUP BY chi_nhanh_id
                ) emp ON emp.chi_nhanh_id = cn.chi_nhanh_id
                CROSS JOIN (
                    SELECT COUNT(*) AS serving_product_count
                    FROM san_pham
                    WHERE loai_san_pham = 'THANH_PHAM'
                      AND trang_thai = 1
                ) prod
                LEFT JOIN (
                    SELECT dh.chi_nhanh_id,
                           COUNT(*) AS order_count,
                           SUM(dh.tong_tien) AS revenue
                    FROM don_hang dh
                    WHERE dh.trang_thai = 'DA_HOAN_THANH'
                      AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
                      AND dh.created_at >= ?
                      AND dh.created_at < ?
                    GROUP BY dh.chi_nhanh_id
                ) ord ON ord.chi_nhanh_id = cn.chi_nhanh_id
                LEFT JOIN (
                    SELECT k.chi_nhanh_id,
                           COUNT(DISTINCT tk.san_pham_id) AS ingredient_count
                    FROM kho k
                    JOIN ton_kho tk ON tk.kho_id = k.kho_id
                    JOIN san_pham sp ON sp.san_pham_id = tk.san_pham_id
                    WHERE sp.loai_san_pham = 'NGUYEN_LIEU'
                    GROUP BY k.chi_nhanh_id
                ) inv ON inv.chi_nhanh_id = cn.chi_nhanh_id
                ORDER BY cn.ten_chi_nhanh
                """;
        List<BranchData> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDate from = startDate == null ? LocalDate.now().minusDays(6) : startDate;
            LocalDate to = endDate == null ? LocalDate.now() : endDate;
            stmt.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("ten_chi_nhanh");
                    rows.add(new BranchData(
                            rs.getString("chi_nhanh_id"),
                            fullName,
                            ReportLookupDAO.normalizeBranchName(fullName),
                            rs.getString("dia_chi"),
                            rs.getString("phone"),
                            rs.getString("manager_name"),
                            rs.getString("manager_phone"),
                            rs.getString("manager_email"),
                            toLocalDate(rs.getTimestamp("created_at")),
                            rs.getInt("trang_thai"),
                            toLocalDate(rs.getTimestamp("updated_at")),
                            "",
                            rs.getInt("employee_count"),
                            rs.getInt("serving_product_count"),
                            rs.getInt("order_count"),
                            nullToZero(rs.getBigDecimal("revenue")),
                            rs.getInt("ingredient_count")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách chi nhánh.", e);
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        LocalDateTime value = timestamp == null ? null : timestamp.toLocalDateTime();
        return value == null ? null : value.toLocalDate();
    }
}
