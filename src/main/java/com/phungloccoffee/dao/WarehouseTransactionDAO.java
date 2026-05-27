package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.WarehouseTransaction;
import com.phungloccoffee.model.WarehouseTransactionDetail;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WarehouseTransactionDAO {
    public void create(WarehouseTransaction transaction, List<WarehouseTransactionDetail> details) throws DatabaseException {
        String sql = """
                INSERT INTO phieu_xuat_kho (phieu_xuat_id, kho_id, nhan_vien_id, ngay_xuat, ly_do_xuat,
                                            so_luong_mat_hang, tong_tien, trang_thai, ghi_chu)
                VALUES (?, ?, ?, SYSTIMESTAMP, ?, ?, 0, 'NHAP', ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transaction.getTransactionCode());
            stmt.setString(2, transaction.getBranchName());
            stmt.setString(3, String.valueOf(transaction.getCreatedBy()));
            stmt.setString(4, transaction.getTransactionType());
            stmt.setInt(5, details == null ? 0 : details.size());
            stmt.setString(6, "Tạo từ màn kho");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tạo phiếu kho.", e);
        }
    }

    public List<WarehouseTransaction> findPendingExports() throws DatabaseException {
        String sql = """
                SELECT px.phieu_xuat_id, px.kho_id, px.trang_thai, px.nhan_vien_id, px.ngay_xuat
                FROM phieu_xuat_kho px
                WHERE px.trang_thai = 'NHAP'
                ORDER BY px.ngay_xuat DESC
                """;
        List<WarehouseTransaction> transactions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                transactions.add(new WarehouseTransaction(
                        0,
                        rs.getString("phieu_xuat_id"),
                        "EXPORT",
                        rs.getString("kho_id"),
                        rs.getString("trang_thai"),
                        0,
                        rs.getTimestamp("ngay_xuat") == null ? null : rs.getTimestamp("ngay_xuat").toLocalDateTime()
                ));
            }
            return transactions;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải phiếu xuất chờ duyệt.", e);
        }
    }

    public List<String> findTransactionCodes() throws DatabaseException {
        String sql = "SELECT phieu_xuat_id FROM phieu_xuat_kho ORDER BY ngay_xuat DESC";
        List<String> codes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                codes.add(rs.getString("phieu_xuat_id"));
            }
            return codes;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải mã phiếu kho.", e);
        }
    }

    public void updateStatus(int transactionId, String status) throws DatabaseException {
        updateStatus(String.valueOf(transactionId), status);
    }

    public void updateStatus(String phieuXuatId, String status) throws DatabaseException {
        String sql = "UPDATE phieu_xuat_kho SET trang_thai = ? WHERE phieu_xuat_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, phieuXuatId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật trạng thái phiếu kho.", e);
        }
    }
}
