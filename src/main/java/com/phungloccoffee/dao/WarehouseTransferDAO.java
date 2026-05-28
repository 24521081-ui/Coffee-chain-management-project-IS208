package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.Kho;
import com.phungloccoffee.model.WarehouseTransferLine;
import com.phungloccoffee.model.WarehouseTransferSlip;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WarehouseTransferDAO {
    private static final String DB_STATUS_PENDING = "NHAP";
    private static final String DB_STATUS_APPROVED = "DA_DUYET";
    private static final String DB_STATUS_REJECTED = "DA_HUY";

    public List<Kho> findActiveWarehouses() throws DatabaseException {
        String sql = """
                SELECT k.kho_id, k.chi_nhanh_id,
                       k.ten_kho || ' - ' || cn.ten_chi_nhanh AS ten_kho,
                       k.trang_thai
                FROM kho k
                JOIN chi_nhanh cn ON cn.chi_nhanh_id = k.chi_nhanh_id
                WHERE k.trang_thai = 1
                ORDER BY cn.ten_chi_nhanh, k.ten_kho
                """;
        List<Kho> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(new Kho(
                        rs.getString("kho_id"),
                        rs.getString("chi_nhanh_id"),
                        rs.getString("ten_kho"),
                        rs.getInt("trang_thai")
                ));
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách kho điều chuyển.", e);
        }
    }

    public Optional<Kho> findWarehouseById(String khoId) throws DatabaseException {
        return findActiveWarehouses().stream().filter(item -> item.getKhoId().equals(khoId)).findFirst();
    }

    public void createTransfer(WarehouseTransferSlip slip) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean sampleSchema = isSampleTransferSchema(conn);
                insertHeader(conn, slip, sampleSchema);
                insertLines(conn, slip, sampleSchema);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Không thể tạo phiếu điều chuyển kho.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tạo phiếu điều chuyển kho.", e);
        }
    }

    public List<WarehouseTransferSlip> findTransfersForApproval(String branchId) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            boolean sampleSchema = isSampleTransferSchema(conn);
            String dateColumn = sampleSchema ? "pdc.ngay_lap" : "pdc.ngay_dieu_chuyen";
            String sql = """
                    SELECT pdc.phieu_dieu_chuyen_id, pdc.kho_nguon_id, pdc.kho_dich_id,
                           pdc.nhan_vien_id, %s AS ngay_tao, pdc.trang_thai,
                           kn.ten_kho || ' - ' || cnn.ten_chi_nhanh AS ten_kho_nguon,
                           kd.ten_kho || ' - ' || cnd.ten_chi_nhanh AS ten_kho_dich
                    FROM phieu_dieu_chuyen_kho pdc
                    JOIN kho kn ON kn.kho_id = pdc.kho_nguon_id
                    JOIN chi_nhanh cnn ON cnn.chi_nhanh_id = kn.chi_nhanh_id
                    JOIN kho kd ON kd.kho_id = pdc.kho_dich_id
                    JOIN chi_nhanh cnd ON cnd.chi_nhanh_id = kd.chi_nhanh_id
                    WHERE pdc.trang_thai = ?
                      AND (? IS NULL OR kn.chi_nhanh_id = ? OR kd.chi_nhanh_id = ?)
                    ORDER BY %s DESC
                    """.formatted(dateColumn, dateColumn);
            List<WarehouseTransferSlip> slips = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, DB_STATUS_PENDING);
                stmt.setString(2, branchId);
                stmt.setString(3, branchId);
                stmt.setString(4, branchId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        WarehouseTransferSlip slip = new WarehouseTransferSlip();
                        slip.setSlipId(rs.getString("phieu_dieu_chuyen_id"));
                        slip.setSourceWarehouseId(rs.getString("kho_nguon_id"));
                        slip.setTargetWarehouseId(rs.getString("kho_dich_id"));
                        slip.setSourceWarehouseName(rs.getString("ten_kho_nguon"));
                        slip.setTargetWarehouseName(rs.getString("ten_kho_dich"));
                        slip.setCreatedBy(rs.getString("nhan_vien_id"));
                        Timestamp createdAt = rs.getTimestamp("ngay_tao");
                        slip.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                        slip.setStatus(DB_STATUS_PENDING);
                        slip.setLines(loadLines(conn, slip.getSlipId(), sampleSchema));
                        slips.add(slip);
                    }
                }
            }
            return slips;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách điều chuyển chờ duyệt.", e);
        }
    }

    public void approveTransfer(String slipId) throws DatabaseException, ValidationException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean sampleSchema = isSampleTransferSchema(conn);
                WarehouseTransferSlip slip = lockTransfer(conn, slipId, sampleSchema);
                for (WarehouseTransferLine line : slip.getLines()) {
                    BigDecimal currentQty = getCurrentInventoryForUpdate(conn, slip.getSourceWarehouseId(), line.getItemId());
                    if (currentQty.compareTo(line.getQuantity()) < 0) {
                        throw new ValidationException("Kho nguồn không còn đủ tồn tại thời điểm duyệt cho nguyên liệu " + line.getItemId() + ".");
                    }
                    updateInventory(conn, slip.getSourceWarehouseId(), line.getItemId(), currentQty.subtract(line.getQuantity()));
                    BigDecimal targetQty = getCurrentInventoryForUpdate(conn, slip.getTargetWarehouseId(), line.getItemId());
                    updateInventory(conn, slip.getTargetWarehouseId(), line.getItemId(), targetQty.add(line.getQuantity()));
                }
                updateStatus(conn, slipId, DB_STATUS_APPROVED, sampleSchema);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Không thể duyệt phiếu điều chuyển kho.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể duyệt phiếu điều chuyển kho.", e);
        }
    }

    public void rejectTransfer(String slipId) throws DatabaseException, ValidationException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean sampleSchema = isSampleTransferSchema(conn);
                lockTransfer(conn, slipId, sampleSchema);
                updateStatus(conn, slipId, DB_STATUS_REJECTED, sampleSchema);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Không thể từ chối phiếu điều chuyển kho.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể từ chối phiếu điều chuyển kho.", e);
        }
    }

    private void insertHeader(Connection conn, WarehouseTransferSlip slip, boolean sampleSchema) throws SQLException {
        String sql = sampleSchema
                ? """
                INSERT INTO phieu_dieu_chuyen_kho (
                    phieu_dieu_chuyen_id, kho_nguon_id, kho_dich_id, nhan_vien_id, ngay_lap, trang_thai, ghi_chu, created_at, updated_at
                ) VALUES (?, ?, ?, ?, SYSTIMESTAMP, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """
                : """
                INSERT INTO phieu_dieu_chuyen_kho (
                    phieu_dieu_chuyen_id, kho_nguon_id, kho_dich_id, nhan_vien_id, ngay_dieu_chuyen, trang_thai
                ) VALUES (?, ?, ?, ?, SYSTIMESTAMP, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slip.getSlipId());
            stmt.setString(2, slip.getSourceWarehouseId());
            stmt.setString(3, slip.getTargetWarehouseId());
            stmt.setString(4, slip.getCreatedBy());
            stmt.setString(5, DB_STATUS_PENDING);
            if (sampleSchema) {
                stmt.setNull(6, Types.CLOB);
            }
            stmt.executeUpdate();
        }
    }

    private void insertLines(Connection conn, WarehouseTransferSlip slip, boolean sampleSchema) throws SQLException {
        String sql = sampleSchema
                ? """
                INSERT INTO chi_tiet_phieu_dieu_chuyen_kho (
                    phieu_dieu_chuyen_id, san_pham_id, so_luong_dieu_chuyen, don_vi_tinh, ghi_chu, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """
                : """
                INSERT INTO chi_tiet_phieu_dieu_chuyen_kho (
                    chi_tiet_dieu_chuyen_id, phieu_dieu_chuyen_id, san_pham_id, so_luong
                ) VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (WarehouseTransferLine line : slip.getLines()) {
                if (sampleSchema) {
                    stmt.setString(1, slip.getSlipId());
                    stmt.setString(2, line.getItemId());
                    stmt.setBigDecimal(3, line.getQuantity());
                    stmt.setString(4, line.getUnit());
                    stmt.setNull(5, Types.CLOB);
                } else {
                    stmt.setString(1, slip.getSlipId() + "-D" + index);
                    stmt.setString(2, slip.getSlipId());
                    stmt.setString(3, line.getItemId());
                    stmt.setBigDecimal(4, line.getQuantity());
                }
                stmt.addBatch();
                index++;
            }
            stmt.executeBatch();
        }
    }

    private List<WarehouseTransferLine> loadLines(Connection conn, String slipId, boolean sampleSchema) throws SQLException {
        String sql = sampleSchema
                ? """
                SELECT ct.san_pham_id, sp.ten_san_pham,
                       NVL(ct.don_vi_tinh, sp.don_vi_tinh) AS don_vi_tinh,
                       ct.so_luong_dieu_chuyen AS so_luong
                FROM chi_tiet_phieu_dieu_chuyen_kho ct
                JOIN san_pham sp ON sp.san_pham_id = ct.san_pham_id
                WHERE ct.phieu_dieu_chuyen_id = ?
                ORDER BY ct.san_pham_id
                """
                : """
                SELECT ct.san_pham_id, sp.ten_san_pham, sp.don_vi_tinh, ct.so_luong
                FROM chi_tiet_phieu_dieu_chuyen_kho ct
                JOIN san_pham sp ON sp.san_pham_id = ct.san_pham_id
                WHERE ct.phieu_dieu_chuyen_id = ?
                ORDER BY ct.chi_tiet_dieu_chuyen_id
                """;
        List<WarehouseTransferLine> lines = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(new WarehouseTransferLine(
                            rs.getString("san_pham_id"),
                            rs.getString("ten_san_pham"),
                            rs.getString("don_vi_tinh"),
                            rs.getBigDecimal("so_luong")
                    ));
                }
            }
        }
        return lines;
    }

    private WarehouseTransferSlip lockTransfer(Connection conn, String slipId, boolean sampleSchema)
            throws SQLException, ValidationException {
        String dateColumn = sampleSchema ? "ngay_lap" : "ngay_dieu_chuyen";
        String sql = """
                SELECT phieu_dieu_chuyen_id, kho_nguon_id, kho_dich_id, nhan_vien_id, %s AS ngay_tao, trang_thai
                FROM phieu_dieu_chuyen_kho
                WHERE phieu_dieu_chuyen_id = ?
                FOR UPDATE
                """.formatted(dateColumn);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Không tìm thấy phiếu điều chuyển kho.");
                }
                if (!DB_STATUS_PENDING.equals(rs.getString("trang_thai"))) {
                    throw new ValidationException("Phiếu này đã được cập nhật trạng thái, vui lòng tải lại danh sách.");
                }
                WarehouseTransferSlip slip = new WarehouseTransferSlip();
                slip.setSlipId(rs.getString("phieu_dieu_chuyen_id"));
                slip.setSourceWarehouseId(rs.getString("kho_nguon_id"));
                slip.setTargetWarehouseId(rs.getString("kho_dich_id"));
                slip.setCreatedBy(rs.getString("nhan_vien_id"));
                Timestamp createdAt = rs.getTimestamp("ngay_tao");
                slip.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                slip.setLines(loadLines(conn, slipId, sampleSchema));
                return slip;
            }
        }
    }

    private void updateStatus(Connection conn, String slipId, String status, boolean sampleSchema) throws SQLException {
        String sql = sampleSchema
                ? "UPDATE phieu_dieu_chuyen_kho SET trang_thai = ?, updated_at = SYSTIMESTAMP WHERE phieu_dieu_chuyen_id = ?"
                : "UPDATE phieu_dieu_chuyen_kho SET trang_thai = ? WHERE phieu_dieu_chuyen_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, slipId);
            stmt.executeUpdate();
        }
    }

    private BigDecimal getCurrentInventoryForUpdate(Connection conn, String khoId, String sanPhamId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT so_luong_ton FROM ton_kho WHERE kho_id = ? AND san_pham_id = ? FOR UPDATE")) {
            stmt.setString(1, khoId);
            stmt.setString(2, sanPhamId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("so_luong_ton") : BigDecimal.ZERO;
            }
        }
    }

    private void updateInventory(Connection conn, String khoId, String sanPhamId, BigDecimal newQuantity) throws SQLException {
        try (PreparedStatement update = conn.prepareStatement(
                "UPDATE ton_kho SET so_luong_ton = ?, last_updated = SYSTIMESTAMP WHERE kho_id = ? AND san_pham_id = ?")) {
            update.setBigDecimal(1, newQuantity);
            update.setString(2, khoId);
            update.setString(3, sanPhamId);
            int updated = update.executeUpdate();
            if (updated == 0) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO ton_kho (kho_id, san_pham_id, so_luong_ton, muc_ton_toi_thieu, last_updated) VALUES (?, ?, ?, 0, SYSTIMESTAMP)")) {
                    insert.setString(1, khoId);
                    insert.setString(2, sanPhamId);
                    insert.setBigDecimal(3, newQuantity);
                    insert.executeUpdate();
                }
            }
        }
    }

    private boolean isSampleTransferSchema(Connection conn) throws SQLException {
        return hasColumn(conn, "phieu_dieu_chuyen_kho", "ngay_lap")
                || hasColumn(conn, "chi_tiet_phieu_dieu_chuyen_kho", "so_luong_dieu_chuyen");
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM user_tab_columns
                WHERE table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase(Locale.ROOT));
            stmt.setString(2, columnName.toUpperCase(Locale.ROOT));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
