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
    public void insert(Connection conn, KhachHang customer) throws DatabaseException {
        String sql = """
                INSERT INTO khach_hang (khach_hang_id, ho_ten, phone, email, created_at, updated_at)
                VALUES (?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getKhachHangId());
            stmt.setString(2, customer.getHoTen());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getEmail());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể thêm khách hàng. Vui lòng kiểm tra lại thông tin.", e);
        }
    }

    public boolean existsByPhone(String phone) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM khach_hang WHERE phone = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể kiểm tra số điện thoại khách hàng: " + e.getMessage(), e);
        }
    }

    public boolean existsByEmail(String email) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM khach_hang WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể kiểm tra email khách hàng: " + e.getMessage(), e);
        }
    }

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
                SELECT khach_hang_id, ho_ten, phone, email,
                       'Thành viên' AS hang_thanh_vien,
                       0 AS diem_tich_luy,
                       NULL AS ghi_chu,
                       created_at, updated_at
                FROM khach_hang
                """);
        List<String> params = new ArrayList<>();
        appendFilters(sql, params, keyword);
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
        appendFilters(sql, params, keyword);

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

    public List<String> findMembershipRanks() {
        return List.of("Thành viên");
    }

    private void appendFilters(StringBuilder sql, List<String> params, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        sql.append(" WHERE (LOWER(ho_ten) LIKE ? OR LOWER(phone) LIKE ? OR LOWER(email) LIKE ?)");
        String like = "%" + keyword.trim().toLowerCase() + "%";
        params.add(like);
        params.add(like);
        params.add(like);
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
