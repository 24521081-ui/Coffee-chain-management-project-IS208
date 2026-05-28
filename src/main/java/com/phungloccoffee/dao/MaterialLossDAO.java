package com.phungloccoffee.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.MaterialLossRecord;
import com.phungloccoffee.util.DBConnection;

public class MaterialLossDAO {
    private static final String STATUS_PENDING = "CHO_DUYET";
    private static final String STATUS_APPROVED = "DA_DUYET";
    private static final String STATUS_REJECTED = "TU_CHOI";

    public List<MaterialLossRecord> findAllLossRecords() throws DatabaseException {
        List<MaterialLossRecord> records = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            boolean richSchema = hasColumn(conn, "hao_hut_nguyen_lieu", "so_luong_hao_hut");
            String sql = richSchema ? richHistorySql() : simpleHistorySql();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MaterialLossRecord record = new MaterialLossRecord();
                    record.setLossId(rs.getString("hao_hut_id"));
                    record.setWarehouseId(getOptionalString(rs, "kho_id"));
                    record.setEmployeeId(getOptionalString(rs, "nhan_vien_id"));
                    record.setMaterialId(rs.getString("san_pham_id"));
                    record.setMaterialName(rs.getString("ten_san_pham"));
                    record.setUnit(rs.getString("don_vi_tinh"));
                    record.setQuantity(rs.getBigDecimal("so_luong"));
                    record.setReason(mergeReasonAndNote(rs.getString("ly_do"), getOptionalString(rs, "ghi_chu")));
                    record.setStatus(getOptionalString(rs, "trang_thai"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    record.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                    records.add(record);
                }
            }
            return records;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải lịch sử hao hụt nguyên liệu.", e);
        }
    }

    public void insert(MaterialLossRecord record) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            boolean richSchema = hasColumn(conn, "hao_hut_nguyen_lieu", "so_luong_hao_hut");
            if (richSchema) {
                insertRich(conn, record);
            } else {
                insertSimple(conn, record);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể lưu bản ghi hao hụt nguyên liệu.", e);
        }
    }

    public void approveLossRecord(String lossId) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensureApprovalSchema(conn);
                MaterialLossRecord record = lockLossRecord(conn, lossId);
                if (!STATUS_PENDING.equals(record.getStatus())) {
                    throw new DatabaseException("Phiếu hao hụt đã được xử lý trước đó.");
                }
                deductInventory(conn, record);
                updateStatus(conn, lossId, STATUS_APPROVED, null);
                conn.commit();
            } catch (SQLException | DatabaseException e) {
                conn.rollback();
                if (e instanceof DatabaseException databaseException) {
                    throw databaseException;
                }
                throw new DatabaseException("Không thể duyệt hao hụt nguyên liệu.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể duyệt hao hụt nguyên liệu.", e);
        }
    }

    public void rejectLossRecord(String lossId, String rejectedReason) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensureApprovalSchema(conn);
                MaterialLossRecord record = lockLossRecord(conn, lossId);
                if (!STATUS_PENDING.equals(record.getStatus())) {
                    throw new DatabaseException("Phiếu hao hụt đã được xử lý trước đó.");
                }
                updateStatus(conn, lossId, STATUS_REJECTED, rejectedReason);
                conn.commit();
            } catch (SQLException | DatabaseException e) {
                conn.rollback();
                if (e instanceof DatabaseException databaseException) {
                    throw databaseException;
                }
                throw new DatabaseException("Không thể từ chối hao hụt nguyên liệu.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể từ chối hao hụt nguyên liệu.", e);
        }
    }

    private String simpleHistorySql() {
        return """
                SELECT hh.hao_hut_id,
                       NULL AS kho_id,
                       NULL AS nhan_vien_id,
                       hh.san_pham_id,
                       sp.ten_san_pham,
                       sp.don_vi_tinh,
                       hh.so_luong,
                       hh.ly_do,
                       NULL AS ghi_chu,
                       hh.created_at
                FROM hao_hut_nguyen_lieu hh
                JOIN san_pham sp ON sp.san_pham_id = hh.san_pham_id
                ORDER BY hh.created_at DESC, hh.hao_hut_id DESC
                """;
    }

    private String richHistorySql() {
        return """
                SELECT hh.hao_hut_id,
                       hh.kho_id,
                       hh.nhan_vien_id,
                       hh.san_pham_id,
                       sp.ten_san_pham,
                       NVL(hh.don_vi_tinh, sp.don_vi_tinh) AS don_vi_tinh,
                       hh.so_luong_hao_hut AS so_luong,
                       hh.ly_do_hao_hut AS ly_do,
                       hh.ghi_chu,
                       hh.trang_thai,
                       CAST(NVL(hh.thoi_gian_ghi_nhan, hh.created_at) AS TIMESTAMP) AS created_at
                FROM hao_hut_nguyen_lieu hh
                JOIN san_pham sp ON sp.san_pham_id = hh.san_pham_id
                ORDER BY NVL(hh.thoi_gian_ghi_nhan, hh.created_at) DESC, hh.hao_hut_id DESC
                """;
    }

    private void insertSimple(Connection conn, MaterialLossRecord record) throws SQLException {
        String sql = """
                INSERT INTO hao_hut_nguyen_lieu (hao_hut_id, san_pham_id, so_luong, ly_do, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getLossId());
            stmt.setString(2, record.getMaterialId());
            stmt.setBigDecimal(3, record.getQuantity());
            setNullableString(stmt, 4, record.getPersistedReason(), Types.NVARCHAR);
            stmt.setTimestamp(5, Timestamp.valueOf(record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt()));
            stmt.executeUpdate();
        }
    }

    private void insertRich(Connection conn, MaterialLossRecord record) throws SQLException {
        String sql = """
                INSERT INTO hao_hut_nguyen_lieu (
                    hao_hut_id, kho_id, san_pham_id, nhan_vien_id, so_luong_hao_hut,
                    don_vi_tinh, ly_do_hao_hut, ghi_chu, thoi_gian_ghi_nhan,
                    trang_thai, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'CHO_DUYET', SYSTIMESTAMP, SYSTIMESTAMP)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getLossId());
            stmt.setString(2, record.getWarehouseId());
            stmt.setString(3, record.getMaterialId());
            stmt.setString(4, record.getEmployeeId());
            stmt.setBigDecimal(5, record.getQuantity());
            stmt.setString(6, record.getUnit());
            setNullableString(stmt, 7, record.getReason(), Types.NVARCHAR);
            setNullableString(stmt, 8, record.getNote(), Types.CLOB);
            stmt.setTimestamp(9, Timestamp.valueOf(record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt()));
            stmt.executeUpdate();
        }
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

    private void ensureApprovalSchema(Connection conn) throws SQLException, DatabaseException {
        if (!hasColumn(conn, "hao_hut_nguyen_lieu", "so_luong_hao_hut")
                || !hasColumn(conn, "hao_hut_nguyen_lieu", "trang_thai")) {
            throw new DatabaseException("Schema hiện tại chưa hỗ trợ duyệt hao hụt nguyên liệu.");
        }
    }

    private MaterialLossRecord lockLossRecord(Connection conn, String lossId) throws SQLException, DatabaseException {
        String sql = """
                SELECT hao_hut_id, kho_id, san_pham_id, nhan_vien_id,
                       so_luong_hao_hut, NVL(don_vi_tinh, '') AS don_vi_tinh,
                       ly_do_hao_hut, ghi_chu, trang_thai,
                       CAST(NVL(thoi_gian_ghi_nhan, created_at) AS TIMESTAMP) AS created_at
                FROM hao_hut_nguyen_lieu
                WHERE hao_hut_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, lossId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new DatabaseException("Không tìm thấy phiếu hao hụt nguyên liệu.");
                }
                MaterialLossRecord record = new MaterialLossRecord();
                record.setLossId(rs.getString("hao_hut_id"));
                record.setWarehouseId(rs.getString("kho_id"));
                record.setMaterialId(rs.getString("san_pham_id"));
                record.setEmployeeId(rs.getString("nhan_vien_id"));
                record.setQuantity(rs.getBigDecimal("so_luong_hao_hut"));
                record.setUnit(rs.getString("don_vi_tinh"));
                record.setReason(rs.getString("ly_do_hao_hut"));
                record.setNote(rs.getString("ghi_chu"));
                record.setStatus(rs.getString("trang_thai"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                record.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                return record;
            }
        }
    }

    private void deductInventory(Connection conn, MaterialLossRecord record) throws SQLException, DatabaseException {
        String selectSql = """
                SELECT so_luong_ton
                FROM ton_kho
                WHERE kho_id = ? AND san_pham_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, record.getWarehouseId());
            stmt.setString(2, record.getMaterialId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new DatabaseException("Không tìm thấy tồn kho của nguyên liệu " + record.getMaterialId() + ".");
                }
                BigDecimal current = rs.getBigDecimal("so_luong_ton");
                BigDecimal quantity = record.getQuantity() == null ? BigDecimal.ZERO : record.getQuantity();
                if (current == null || current.compareTo(quantity) < 0) {
                    throw new DatabaseException("Tồn kho hiện tại không đủ để duyệt hao hụt.");
                }
            }
        }

        String updateSql = """
                UPDATE ton_kho
                SET so_luong_ton = so_luong_ton - ?, last_updated = SYSTIMESTAMP
                WHERE kho_id = ? AND san_pham_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setBigDecimal(1, record.getQuantity());
            stmt.setString(2, record.getWarehouseId());
            stmt.setString(3, record.getMaterialId());
            if (stmt.executeUpdate() == 0) {
                throw new DatabaseException("Không thể cập nhật tồn kho khi duyệt hao hụt.");
            }
        }
    }

    private void updateStatus(Connection conn, String lossId, String status, String rejectedReason) throws SQLException {
        String sql = """
                UPDATE hao_hut_nguyen_lieu
                SET trang_thai = ?, ghi_chu = ?, updated_at = SYSTIMESTAMP
                WHERE hao_hut_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, composeNote(status, rejectedReason));
            stmt.setString(3, lossId);
            stmt.executeUpdate();
        }
    }

    private void setNullableString(PreparedStatement stmt, int index, String value, int sqlType) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(index, sqlType);
        } else {
            stmt.setString(index, value.trim());
        }
    }

    private String getOptionalString(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getString(columnName);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private String mergeReasonAndNote(String reason, String note) {
        String cleanReason = reason == null ? "" : reason.trim();
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.isBlank()) {
            return cleanReason;
        }
        if (cleanReason.isBlank()) {
            return cleanNote;
        }
        return cleanReason + " | Ghi chú: " + cleanNote;
    }

    private String composeNote(String status, String rejectedReason) {
        if (STATUS_REJECTED.equals(status) && rejectedReason != null && !rejectedReason.isBlank()) {
            return "Từ chối: " + rejectedReason.trim();
        }
        if (STATUS_APPROVED.equals(status)) {
            return "Đã duyệt và cập nhật tồn kho";
        }
        return null;
    }
}
