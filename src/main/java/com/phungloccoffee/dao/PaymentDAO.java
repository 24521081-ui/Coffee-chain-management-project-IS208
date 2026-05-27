package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.Payment;
import com.phungloccoffee.model.PaymentHistory;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    public void save(Payment payment) throws DatabaseException {
        save(String.valueOf(payment.getOrderId()), payment.getMethod(), payment.getAmount());
    }

    public void save(String orderId, String method, BigDecimal amount) throws DatabaseException {
        String sql = "UPDATE don_hang SET trang_thai_thanh_toan = 'DA_THANH_TOAN', updated_at = SYSTIMESTAMP WHERE don_hang_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Khong tim thay don hang de thanh toan: " + orderId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Khong the luu thanh toan.", e);
        }
    }

    public List<PaymentHistory> findHistory() throws DatabaseException {
        String sql = """
                SELECT dh.don_hang_id, nv.ho_ten AS cashier_name, dh.tong_tien, dh.updated_at, dh.trang_thai_thanh_toan
                FROM don_hang dh
                LEFT JOIN nhan_vien nv ON nv.nhan_vien_id = dh.nhan_vien_id
                ORDER BY dh.updated_at DESC
                """;
        List<PaymentHistory> histories = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                histories.add(new PaymentHistory(
                        rs.getString("don_hang_id"),
                        rs.getString("cashier_name"),
                        "POS",
                        rs.getBigDecimal("tong_tien"),
                        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime(),
                        rs.getString("trang_thai_thanh_toan")
                ));
            }
            return histories;
        } catch (SQLException e) {
            throw new DatabaseException("Khong the tai lich su thanh toan.", e);
        }
    }
}
