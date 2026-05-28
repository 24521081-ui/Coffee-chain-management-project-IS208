package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.SupplierDirectoryItem;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {
    public List<SupplierDirectoryItem> findAllSuppliers() throws DatabaseException {
        String sql = """
                SELECT ncc.nha_cung_cap_id,
                       ncc.ten_nha_cung_cap,
                       NVL(cat.ten_danh_muc, N'Chưa phân loại') AS nhom_hang,
                       ncc.phone,
                       ncc.email,
                       ncc.trang_thai
                FROM nha_cung_cap ncc
                LEFT JOIN (
                    SELECT nha_cung_cap_id, ten_danh_muc
                    FROM (
                        SELECT pnk.nha_cung_cap_id,
                               dm.ten_danh_muc,
                               ROW_NUMBER() OVER (
                                   PARTITION BY pnk.nha_cung_cap_id
                                   ORDER BY COUNT(*) DESC, dm.ten_danh_muc
                               ) AS rn
                        FROM phieu_nhap_kho pnk
                        JOIN chi_tiet_nhap_kho ctnk ON ctnk.phieu_nhap_id = pnk.phieu_nhap_id
                        JOIN san_pham sp ON sp.san_pham_id = ctnk.san_pham_id
                        LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                        GROUP BY pnk.nha_cung_cap_id, dm.ten_danh_muc
                    )
                    WHERE rn = 1
                ) cat ON cat.nha_cung_cap_id = ncc.nha_cung_cap_id
                ORDER BY ncc.ten_nha_cung_cap
                """;
        List<SupplierDirectoryItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                items.add(new SupplierDirectoryItem(
                        rs.getString("nha_cung_cap_id"),
                        rs.getString("ten_nha_cung_cap"),
                        rs.getString("nhom_hang"),
                        phone == null || phone.isBlank() ? "Chưa cập nhật" : phone,
                        phone == null || phone.isBlank() ? "Chưa cập nhật" : phone,
                        email == null || email.isBlank() ? "Chưa cập nhật" : email,
                        rs.getInt("trang_thai")
                ));
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách nhà cung cấp.", e);
        }
    }
}
