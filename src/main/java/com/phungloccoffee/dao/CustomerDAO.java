package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.KhachHang;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    public int countAllCustomers() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM khach_hang";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể đếm tổng khách hàng: " + e.getMessage(), e);
        }
    }

    public int countNewCustomersThisMonth() throws DatabaseException {
        String sql = """
                SELECT COUNT(*)
                FROM khach_hang
                WHERE created_at >= TRUNC(SYSTIMESTAMP, 'MM')
                  AND created_at < ADD_MONTHS(TRUNC(SYSTIMESTAMP, 'MM'), 1)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể đếm khách mới tháng này: " + e.getMessage(), e);
        }
    }

    public List<KhachHang> findCustomers(String keyword, String rank, int offset, int pageSize) throws DatabaseException {
        StringBuilder sql = new StringBuilder("""
                SELECT khach_hang_id, ho_ten, phone, email, hang_thanh_vien,
                       diem_tich_luy, ghi_chu, created_at, updated_at
                FROM khach_hang
                """);
        List<String> params = new ArrayList<>();
        appendFilters(sql, params, keyword, rank);
        sql.append(" ORDER BY created_at DESC, khach_hang_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<KhachHang> customers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = bindParams(stmt, params);
            stmt.setInt(index++, offset);
            stmt.setInt(index, pageSize);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(map(rs));
                }
            }
            return customers;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách khách hàng: " + e.getMessage(), e);
        }
    }

    public int countCustomersByFilter(String keyword, String rank) throws DatabaseException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM khach_hang");
        List<String> params = new ArrayList<>();
        appendFilters(sql, params, keyword, rank);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể đếm khách hàng theo bộ lọc: " + e.getMessage(), e);
        }
    }

    public List<String> findMembershipRanks() throws DatabaseException {
        String sql = """
                SELECT DISTINCT hang_thanh_vien
                FROM khach_hang
                WHERE hang_thanh_vien IS NOT NULL
                ORDER BY hang_thanh_vien
                """;
        List<String> ranks = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ranks.add(rs.getString("hang_thanh_vien"));
            }
            return ranks;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải hạng thành viên: " + e.getMessage(), e);
        }
    }

    private void appendFilters(StringBuilder sql, List<String> params, String keyword, String rank) {
        List<String> conditions = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            conditions.add("(LOWER(ho_ten) LIKE ? OR LOWER(phone) LIKE ? OR LOWER(email) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (rank != null && !rank.isBlank()) {
            conditions.add("hang_thanh_vien = ?");
            params.add(rank.trim());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", conditions));
        }
    }

    private int bindParams(PreparedStatement stmt, List<String> params) throws SQLException {
        int index = 1;
        for (String param : params) {
            stmt.setString(index++, param);
        }
        return index;
    }

    private KhachHang map(ResultSet rs) throws SQLException {
        return new KhachHang(
                rs.getString("khach_hang_id"),
                rs.getString("ho_ten"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("hang_thanh_vien"),
                rs.getInt("diem_tich_luy"),
                rs.getString("ghi_chu"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
