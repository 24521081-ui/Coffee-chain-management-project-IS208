package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InventoryDAO {
    public List<InventoryItem> findAll() throws DatabaseException {
        return findByKho(null);
    }

    public List<InventoryItem> findByKho(String khoId) throws DatabaseException {
        String sql = """
                SELECT tk.kho_id, tk.san_pham_id, sp.ten_san_pham, sp.don_vi_tinh,
                       tk.so_luong_ton, tk.muc_ton_toi_thieu
                FROM ton_kho tk
                JOIN san_pham sp ON sp.san_pham_id = tk.san_pham_id
                WHERE (? IS NULL OR tk.kho_id = ?)
                ORDER BY sp.ten_san_pham
                """;
        List<InventoryItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, khoId);
            stmt.setString(2, khoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new InventoryItem(
                            0,
                            rs.getString("san_pham_id"),
                            rs.getString("ten_san_pham"),
                            rs.getString("don_vi_tinh"),
                            rs.getBigDecimal("so_luong_ton"),
                            rs.getBigDecimal("muc_ton_toi_thieu")
                    ));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải tồn kho.", e);
        }
    }

    public Optional<InventoryItem> findByKhoAndSanPham(String khoId, String sanPhamId) throws DatabaseException {
        return findByKho(khoId).stream().filter(item -> sanPhamId.equals(item.getItemCode())).findFirst();
    }

    public void updateSoLuongTon(String khoId, String sanPhamId, BigDecimal newQuantity) throws DatabaseException {
        String sql = "UPDATE ton_kho SET so_luong_ton = ?, last_updated = SYSTIMESTAMP WHERE kho_id = ? AND san_pham_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newQuantity);
            stmt.setString(2, khoId);
            stmt.setString(3, sanPhamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật tồn kho.", e);
        }
    }

    public boolean hasEnoughStock(String khoId, String sanPhamId, BigDecimal quantity) throws DatabaseException {
        return findByKhoAndSanPham(khoId, sanPhamId)
                .map(item -> item.getQuantityOnHand().compareTo(quantity) >= 0)
                .orElse(false);
    }

    public BranchInventorySnapshot findByBranch(String branchId) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            String khoId = findActiveKhoIdByBranch(conn, branchId)
                    .orElseThrow(() -> new DatabaseException("Không tìm thấy kho đang hoạt động của chi nhánh " + branchId + "."));
            return new BranchInventorySnapshot(khoId, findByKho(khoId));
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải tồn kho chi nhánh: " + e.getMessage(), e);
        }
    }

    public Optional<String> findActiveKhoIdByBranch(Connection conn, String chiNhanhId) throws DatabaseException {
        String sql = """
                SELECT kho_id
                FROM kho
                WHERE chi_nhanh_id = ? AND trang_thai = 1
                ORDER BY kho_id
                FETCH FIRST 1 ROWS ONLY
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chiNhanhId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("kho_id"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tìm kho của chi nhánh: " + e.getMessage(), e);
        }
    }

    public List<StockDeduction> findRecipeDeductions(Connection conn, String sanPhamId) throws DatabaseException {
        String sql = """
                SELECT ctdm.san_pham_nguyen_lieu, SUM(ctdm.so_luong_dung) AS so_luong_can_tru
                FROM dinh_muc_san_pham dm
                JOIN chi_tiet_dinh_muc ctdm ON ctdm.dinh_muc_id = dm.dinh_muc_id
                WHERE dm.san_pham_ban_id = ? AND dm.trang_thai = 1
                GROUP BY ctdm.san_pham_nguyen_lieu
                """;
        List<StockDeduction> deductions = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sanPhamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    deductions.add(new StockDeduction(
                            rs.getString("san_pham_nguyen_lieu"),
                            rs.getBigDecimal("so_luong_can_tru")
                    ));
                }
            }
            return deductions;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải định mức sản phẩm: " + e.getMessage(), e);
        }
    }

    public Map<String, List<StockDeduction>> findAllRecipeDeductions() throws DatabaseException {
        String sql = """
                SELECT dm.san_pham_ban_id, ctdm.san_pham_nguyen_lieu, SUM(ctdm.so_luong_dung) AS so_luong_can_tru
                FROM dinh_muc_san_pham dm
                JOIN chi_tiet_dinh_muc ctdm ON ctdm.dinh_muc_id = dm.dinh_muc_id
                WHERE dm.trang_thai = 1
                GROUP BY dm.san_pham_ban_id, ctdm.san_pham_nguyen_lieu
                ORDER BY dm.san_pham_ban_id, ctdm.san_pham_nguyen_lieu
                """;
        Map<String, List<StockDeduction>> recipes = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String productId = rs.getString("san_pham_ban_id");
                recipes.computeIfAbsent(productId, ignored -> new ArrayList<>())
                        .add(new StockDeduction(
                                rs.getString("san_pham_nguyen_lieu"),
                                rs.getBigDecimal("so_luong_can_tru")
                        ));
            }
            return recipes;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải toàn bộ định mức sản phẩm: " + e.getMessage(), e);
        }
    }

    public void deductStock(Connection conn, String khoId, String sanPhamId, BigDecimal quantity) throws DatabaseException {
        String selectSql = """
                SELECT so_luong_ton
                FROM ton_kho
                WHERE kho_id = ? AND san_pham_id = ?
                FOR UPDATE
                """;
        String updateSql = """
                UPDATE ton_kho
                SET so_luong_ton = so_luong_ton - ?, last_updated = SYSTIMESTAMP
                WHERE kho_id = ? AND san_pham_id = ?
                """;
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, khoId);
            selectStmt.setString(2, sanPhamId);
            BigDecimal quantityOnHand;
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new DatabaseException("Chưa có tồn kho cho sản phẩm/nguyên liệu " + sanPhamId + " tại kho " + khoId + ".");
                }
                quantityOnHand = rs.getBigDecimal("so_luong_ton");
            }

            if (quantityOnHand == null || quantityOnHand.compareTo(quantity) < 0) {
                throw new DatabaseException("Tồn kho không đủ cho sản phẩm/nguyên liệu " + sanPhamId + ".");
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBigDecimal(1, quantity);
                updateStmt.setString(2, khoId);
                updateStmt.setString(3, sanPhamId);
                int updated = updateStmt.executeUpdate();
                if (updated == 0) {
                    throw new DatabaseException("Không thể trừ tồn kho cho sản phẩm/nguyên liệu " + sanPhamId + ".");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể trừ tồn kho: " + e.getMessage(), e);
        }
    }

    public static class StockDeduction {
        private final String sanPhamId;
        private final BigDecimal quantity;

        public StockDeduction(String sanPhamId, BigDecimal quantity) {
            this.sanPhamId = sanPhamId;
            this.quantity = quantity;
        }

        public String getSanPhamId() {
            return sanPhamId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }
    }

    public static class BranchInventorySnapshot {
        private final String khoId;
        private final List<InventoryItem> items;

        public BranchInventorySnapshot(String khoId, List<InventoryItem> items) {
            this.khoId = khoId;
            this.items = items;
        }

        public String getKhoId() {
            return khoId;
        }

        public List<InventoryItem> getItems() {
            return items;
        }
    }
}
