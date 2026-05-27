package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChiTietDonHangDAO {
    public void insert(OrderDetail detail) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            insert(conn, detail);
        } catch (SQLException e) {
            throw new DatabaseException("Khong the them chi tiet don hang: " + e.getMessage(), e);
        }
    }

    public void insert(Connection conn, OrderDetail detail) throws DatabaseException {
        String sql = """
                INSERT INTO chi_tiet_don_hang (chi_tiet_don_hang_id, don_hang_id, san_pham_id, so_luong,
                                               don_gia, thanh_tien, ghi_chu, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, detail.getChiTietDonHangId());
            stmt.setString(2, detail.getDonHangId());
            stmt.setString(3, detail.getSanPhamId());
            stmt.setBigDecimal(4, detail.getSoLuong());
            stmt.setBigDecimal(5, detail.getDonGia());
            stmt.setBigDecimal(6, detail.getThanhTien());
            stmt.setString(7, detail.getGhiChu());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Khong the them chi tiet don hang: " + e.getMessage(), e);
        }
    }

    public List<OrderDetail> findByDonHangId(String donHangId) throws DatabaseException {
        String sql = "SELECT * FROM chi_tiet_don_hang WHERE don_hang_id = ?";
        List<OrderDetail> details = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, donHangId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    details.add(new OrderDetail(
                            rs.getString("chi_tiet_don_hang_id"),
                            rs.getString("don_hang_id"),
                            rs.getString("san_pham_id"),
                            rs.getBigDecimal("so_luong"),
                            rs.getBigDecimal("don_gia"),
                            rs.getBigDecimal("thanh_tien"),
                            rs.getString("ghi_chu"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("updated_at"))
                    ));
                }
            }
            return details;
        } catch (SQLException e) {
            throw new DatabaseException("Khong the them chi tiet don hang: " + e.getMessage(), e);
        }
    }

    public List<OrderDetail> findByDonHangId(Connection conn, String donHangId) throws DatabaseException {
        String sql = "SELECT * FROM chi_tiet_don_hang WHERE don_hang_id = ?";
        List<OrderDetail> details = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, donHangId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    details.add(new OrderDetail(
                            rs.getString("chi_tiet_don_hang_id"),
                            rs.getString("don_hang_id"),
                            rs.getString("san_pham_id"),
                            rs.getBigDecimal("so_luong"),
                            rs.getBigDecimal("don_gia"),
                            rs.getBigDecimal("thanh_tien"),
                            rs.getString("ghi_chu"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("updated_at"))
                    ));
                }
            }
            return details;
        } catch (SQLException e) {
            throw new DatabaseException("Khong the tai chi tiet don hang: " + e.getMessage(), e);
        }
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
