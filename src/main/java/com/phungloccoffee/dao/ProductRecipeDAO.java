package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.product.ProductCatalogModels.Ingredient;
import com.phungloccoffee.model.product.ProductCatalogModels.ProductRecipe;
import com.phungloccoffee.model.product.ProductCatalogModels.RecipeDisplayRow;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.phungloccoffee.model.product.ProductCatalogModels.RECIPE_ACTIVE;
import static com.phungloccoffee.model.product.ProductCatalogModels.RECIPE_REVIEW;

public class ProductRecipeDAO {
    public List<Ingredient> findIngredients() throws DatabaseException {
        String sql = """
                SELECT san_pham_id, ten_san_pham, don_vi_tinh
                FROM san_pham
                WHERE loai_san_pham = 'NGUYEN_LIEU'
                  AND trang_thai = 1
                ORDER BY ten_san_pham
                """;
        List<Ingredient> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new Ingredient(
                        rs.getString("san_pham_id"),
                        rs.getString("ten_san_pham"),
                        rs.getString("don_vi_tinh")
                ));
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách nguyên liệu.", e);
        }
    }

    public List<ProductRecipe> findRecipeRows(String productCode) throws DatabaseException {
        String sql = """
                SELECT ctdm.nguyen_lieu_id,
                       nl.ten_san_pham AS ten_nguyen_lieu,
                       ctdm.so_luong,
                       nl.don_vi_tinh,
                       dm.trang_thai
                FROM dinh_muc_san_pham dm
                JOIN chi_tiet_dinh_muc ctdm ON ctdm.dinh_muc_id = dm.dinh_muc_id
                JOIN san_pham nl ON nl.san_pham_id = ctdm.nguyen_lieu_id
                WHERE dm.san_pham_id = ?
                ORDER BY nl.ten_san_pham
                """;
        List<ProductRecipe> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProductRecipe(
                            rs.getString("nguyen_lieu_id"),
                            rs.getString("ten_nguyen_lieu"),
                            rs.getBigDecimal("so_luong"),
                            rs.getString("don_vi_tinh"),
                            rs.getInt("trang_thai") == 1 ? RECIPE_ACTIVE : RECIPE_REVIEW
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải định mức sản phẩm.", e);
        }
    }

    public List<RecipeDisplayRow> findAllRecipeDisplayRows() throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id,
                       sp.ten_san_pham,
                       nl.ten_san_pham AS ten_nguyen_lieu,
                       ctdm.so_luong,
                       nl.don_vi_tinh,
                       dm.trang_thai
                FROM dinh_muc_san_pham dm
                JOIN san_pham sp ON sp.san_pham_id = dm.san_pham_id
                JOIN chi_tiet_dinh_muc ctdm ON ctdm.dinh_muc_id = dm.dinh_muc_id
                JOIN san_pham nl ON nl.san_pham_id = ctdm.nguyen_lieu_id
                ORDER BY sp.ten_san_pham, nl.ten_san_pham
                """;
        List<RecipeDisplayRow> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                BigDecimal quantity = rs.getBigDecimal("so_luong");
                rows.add(new RecipeDisplayRow(
                        rs.getString("san_pham_id"),
                        rs.getString("ten_san_pham"),
                        rs.getString("ten_nguyen_lieu"),
                        quantity == null ? "0" : quantity.stripTrailingZeros().toPlainString(),
                        rs.getString("don_vi_tinh"),
                        rs.getInt("trang_thai") == 1 ? RECIPE_ACTIVE : RECIPE_REVIEW
                ));
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách định mức.", e);
        }
    }

    public boolean hasRecipe(String productCode) throws DatabaseException {
        String sql = """
                SELECT COUNT(*)
                FROM dinh_muc_san_pham dm
                JOIN chi_tiet_dinh_muc ctdm ON ctdm.dinh_muc_id = dm.dinh_muc_id
                WHERE dm.san_pham_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể kiểm tra định mức sản phẩm.", e);
        }
    }

    public void replaceRecipeRows(String productCode, List<ProductRecipe> rows) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String recipeId = findRecipeId(conn, productCode);
                if (recipeId != null) {
                    deleteRecipeDetails(conn, recipeId);
                } else if (rows != null && !rows.isEmpty()) {
                    recipeId = nextCode(conn, "dinh_muc_san_pham", "dinh_muc_id", "DM", 4);
                    insertRecipeHeader(conn, recipeId, productCode);
                }

                if (recipeId != null && rows != null) {
                    for (ProductRecipe row : rows) {
                        insertRecipeDetail(conn, recipeId, row);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể cập nhật định mức sản phẩm.", e);
        }
    }

    private String findRecipeId(Connection conn, String productCode) throws SQLException {
        String sql = "SELECT dinh_muc_id FROM dinh_muc_san_pham WHERE san_pham_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("dinh_muc_id") : null;
            }
        }
    }

    private void insertRecipeHeader(Connection conn, String recipeId, String productCode) throws SQLException {
        String sql = "INSERT INTO dinh_muc_san_pham (dinh_muc_id, san_pham_id, trang_thai) VALUES (?, ?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, recipeId);
            stmt.setString(2, productCode);
            stmt.executeUpdate();
        }
    }

    private void deleteRecipeDetails(Connection conn, String recipeId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM chi_tiet_dinh_muc WHERE dinh_muc_id = ?")) {
            stmt.setString(1, recipeId);
            stmt.executeUpdate();
        }
    }

    private void insertRecipeDetail(Connection conn, String recipeId, ProductRecipe row) throws SQLException {
        String sql = """
                INSERT INTO chi_tiet_dinh_muc (chi_tiet_dinh_muc_id, dinh_muc_id, nguyen_lieu_id, so_luong)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nextCode(conn, "chi_tiet_dinh_muc", "chi_tiet_dinh_muc_id", "CTDM", 4));
            stmt.setString(2, recipeId);
            stmt.setString(3, row.materialCode());
            stmt.setBigDecimal(4, row.quantity());
            stmt.executeUpdate();
        }
    }

    private String nextCode(Connection conn, String tableName, String columnName, String prefix, int width) throws SQLException {
        String sql = "SELECT " + columnName + " FROM " + tableName + " WHERE " + columnName + " LIKE ?";
        int max = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String value = rs.getString(1);
                    if (value == null || !value.toUpperCase(Locale.ROOT).startsWith(prefix)) {
                        continue;
                    }
                    String digits = value.substring(prefix.length()).replaceAll("\\D+", "");
                    if (!digits.isEmpty()) {
                        max = Math.max(max, Integer.parseInt(digits));
                    }
                }
            }
        }
        return prefix + String.format(Locale.ROOT, "%0" + width + "d", max + 1);
    }
}
