package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.Product;
import com.phungloccoffee.model.ProductCategory;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {
    public List<Product> findActiveProducts() throws DatabaseException {
        return findAllActive();
    }

    public List<Product> findProductsForPOS() throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id, sp.danh_muc_id, dm.ten_danh_muc, sp.ten_san_pham,
                       sp.loai_san_pham, sp.don_vi_tinh, sp.gia_ban, sp.gia_von,
                       sp.trang_thai, sp.created_at, sp.updated_at
                FROM san_pham sp
                LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                WHERE sp.loai_san_pham = 'THANH_PHAM'
                   OR UPPER(sp.loai_san_pham) = 'TOPPING'
                   OR UPPER(dm.ten_danh_muc) = 'TOPPING'
                ORDER BY dm.ten_danh_muc, sp.ten_san_pham
                """;
        return findProducts(sql, null);
    }

    public List<Product> findAllActive() throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id, sp.danh_muc_id, dm.ten_danh_muc, sp.ten_san_pham,
                       sp.loai_san_pham, sp.don_vi_tinh, sp.gia_ban, sp.gia_von,
                       sp.trang_thai, sp.created_at, sp.updated_at
                FROM san_pham sp
                LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                WHERE sp.loai_san_pham = 'THANH_PHAM' AND sp.trang_thai = 1
                ORDER BY sp.ten_san_pham
                """;
        return findProducts(sql, null);
    }

    public List<Product> findAll() throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id, sp.danh_muc_id, dm.ten_danh_muc, sp.ten_san_pham,
                       sp.loai_san_pham, sp.don_vi_tinh, sp.gia_ban, sp.gia_von,
                       sp.trang_thai, sp.created_at, sp.updated_at
                FROM san_pham sp
                LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                ORDER BY sp.ten_san_pham
                """;
        return findProducts(sql, null);
    }

    public Optional<Product> findById(String sanPhamId) throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id, sp.danh_muc_id, dm.ten_danh_muc, sp.ten_san_pham,
                       sp.loai_san_pham, sp.don_vi_tinh, sp.gia_ban, sp.gia_von,
                       sp.trang_thai, sp.created_at, sp.updated_at
                FROM san_pham sp
                LEFT JOIN danh_muc_san_pham dm ON dm.danh_muc_id = sp.danh_muc_id
                WHERE sp.san_pham_id = ?
                """;
        List<Product> products = findProducts(sql, sanPhamId);
        return products.stream().findFirst();
    }

    public List<Product> searchByKeyword(String keyword) throws DatabaseException {
        String sql = """
                SELECT san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh,
                       gia_ban, gia_von, trang_thai, created_at, updated_at
                FROM san_pham
                WHERE LOWER(ten_san_pham) LIKE ? OR LOWER(san_pham_id) LIKE ?
                ORDER BY ten_san_pham
                """;
        List<Product> products = new ArrayList<>();
        String like = "%" + (keyword == null ? "" : keyword.toLowerCase()) + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, like);
            stmt.setString(2, like);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
            return products;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tìm sản phẩm.", e);
        }
    }

    public List<ProductCategory> findCategories() throws DatabaseException {
        return new DanhMucSanPhamDAO().findAll();
    }

    public void save(Product product) throws DatabaseException {
        if (findById(product.getSanPhamId()).isPresent()) {
            update(product);
        } else {
            insert(product);
        }
    }

    public void insert(Product sp) throws DatabaseException {
        String sql = """
                INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh,
                                      gia_ban, gia_von, trang_thai, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindProduct(stmt, sp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể thêm sản phẩm.", e);
        }
    }

    public void update(Product sp) throws DatabaseException {
        String sql = """
                UPDATE san_pham
                SET danh_muc_id = ?, ten_san_pham = ?, loai_san_pham = ?, don_vi_tinh = ?,
                    gia_ban = ?, gia_von = ?, trang_thai = ?, updated_at = SYSTIMESTAMP
                WHERE san_pham_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sp.getDanhMucId());
            stmt.setString(2, sp.getTenSanPham());
            stmt.setString(3, sp.getLoaiSanPham());
            stmt.setString(4, sp.getDonViTinh());
            stmt.setBigDecimal(5, sp.getGiaBan());
            stmt.setBigDecimal(6, sp.getGiaVon());
            stmt.setInt(7, sp.getTrangThai());
            stmt.setString(8, sp.getSanPhamId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật sản phẩm.", e);
        }
    }

    public void updateInfo(Product sp) throws DatabaseException {
        String sql = """
                UPDATE san_pham
                SET danh_muc_id = ?, ten_san_pham = ?, updated_at = SYSTIMESTAMP
                WHERE san_pham_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sp.getDanhMucId());
            stmt.setString(2, sp.getTenSanPham());
            stmt.setString(3, sp.getSanPhamId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật thông tin sản phẩm.", e);
        }
    }

    public void updatePrice(String sanPhamId, java.math.BigDecimal price) throws DatabaseException {
        String sql = "UPDATE san_pham SET gia_ban = ?, updated_at = SYSTIMESTAMP WHERE san_pham_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, price);
            stmt.setString(2, sanPhamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật giá bán sản phẩm.", e);
        }
    }

    public void updateStatus(String sanPhamId, int status) throws DatabaseException {
        String sql = "UPDATE san_pham SET trang_thai = ?, updated_at = SYSTIMESTAMP WHERE san_pham_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, status);
            stmt.setString(2, sanPhamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật trạng thái sản phẩm.", e);
        }
    }

    private void bindProduct(PreparedStatement stmt, Product sp) throws SQLException {
        stmt.setString(1, sp.getSanPhamId());
        stmt.setString(2, sp.getDanhMucId());
        stmt.setString(3, sp.getTenSanPham());
        stmt.setString(4, sp.getLoaiSanPham());
        stmt.setString(5, sp.getDonViTinh());
        stmt.setBigDecimal(6, sp.getGiaBan());
        stmt.setBigDecimal(7, sp.getGiaVon());
        stmt.setInt(8, sp.getTrangThai());
    }

    private List<Product> findProducts(String sql, String idParam) throws DatabaseException {
        List<Product> products = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (idParam != null) {
                stmt.setString(1, idParam);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
            return products;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải sản phẩm.", e);
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getString("san_pham_id"),
                rs.getString("danh_muc_id"),
                readOptionalString(rs, "ten_danh_muc"),
                rs.getString("ten_san_pham"),
                rs.getString("loai_san_pham"),
                rs.getString("don_vi_tinh"),
                rs.getBigDecimal("gia_ban"),
                rs.getBigDecimal("gia_von"),
                rs.getInt("trang_thai"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String readOptionalString(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
