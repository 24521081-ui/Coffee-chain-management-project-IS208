package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.MaterialLossRecord;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MaterialLossDAO {
    public List<MaterialLossRecord> findAllLossRecords() throws DatabaseException {
        String sql = """
                SELECT hh.hao_hut_id,
                       hh.san_pham_id,
                       sp.ten_san_pham,
                       sp.don_vi_tinh,
                       hh.so_luong,
                       hh.ly_do,
                       hh.created_at
                FROM hao_hut_nguyen_lieu hh
                JOIN san_pham sp ON sp.san_pham_id = hh.san_pham_id
                ORDER BY hh.created_at DESC, hh.hao_hut_id DESC
                """;
        List<MaterialLossRecord> records = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                MaterialLossRecord record = new MaterialLossRecord();
                record.setLossId(rs.getString("hao_hut_id"));
                record.setMaterialId(rs.getString("san_pham_id"));
                record.setMaterialName(rs.getString("ten_san_pham"));
                record.setUnit(rs.getString("don_vi_tinh"));
                record.setQuantity(rs.getBigDecimal("so_luong"));
                record.setReason(rs.getString("ly_do"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                record.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                records.add(record);
            }
            return records;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải lịch sử hao hụt nguyên liệu.", e);
        }
    }

    public void insert(MaterialLossRecord record) throws DatabaseException {
        String sql = """
                INSERT INTO hao_hut_nguyen_lieu (hao_hut_id, san_pham_id, so_luong, ly_do, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getLossId());
            stmt.setString(2, record.getMaterialId());
            stmt.setBigDecimal(3, record.getQuantity());
            if (record.getPersistedReason() == null || record.getPersistedReason().isBlank()) {
                stmt.setNull(4, Types.NVARCHAR);
            } else {
                stmt.setString(4, record.getPersistedReason());
            }
            stmt.setTimestamp(5, Timestamp.valueOf(record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể lưu bản ghi hao hụt nguyên liệu.", e);
        }
    }
}
