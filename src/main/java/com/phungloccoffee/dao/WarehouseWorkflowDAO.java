package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.NhaCungCap;
import com.phungloccoffee.model.WarehouseApprovalItem;
import com.phungloccoffee.model.WarehouseSlip;
import com.phungloccoffee.model.WarehouseSlipLine;
import com.phungloccoffee.model.WarehouseSlipStatus;
import com.phungloccoffee.model.WarehouseSlipType;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseWorkflowDAO {
    private static final String NOTE_DRAFT_PREFIX = "[DRAFT]";
    private static final String NOTE_PENDING_PREFIX = "[PENDING_APPROVAL]";
    private static final String NOTE_PENDING_PREFIX_LEGACY = "[PENDING_APPROCVAL]";
    private static final String DB_STATUS_OPEN = "NHAP";
    private static final String DB_STATUS_APPROVED = "DA_DUYET";
    private static final String DB_STATUS_STOCKTAKE_APPROVED = "DA_HOAN_THANH";
    private static final String DB_STATUS_REJECTED = "DA_HUY";

    public List<NhaCungCap> findActiveSuppliers() throws DatabaseException {
        String sql = """
                SELECT nha_cung_cap_id, ten_nha_cung_cap, phone, email, trang_thai
                FROM nha_cung_cap
                WHERE trang_thai = 1
                ORDER BY ten_nha_cung_cap
                """;
        List<NhaCungCap> suppliers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                suppliers.add(new NhaCungCap(
                        rs.getString("nha_cung_cap_id"),
                        rs.getString("ten_nha_cung_cap"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getInt("trang_thai")
                ));
            }
            return suppliers;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải nhà cung cấp.", e);
        }
    }

    public Optional<String> findDefaultWarehouseByBranch(String branchId) throws DatabaseException {
        String sql = """
                SELECT kho_id
                FROM kho
                WHERE trang_thai = 1
                  AND (? IS NULL OR chi_nhanh_id = ?)
                ORDER BY kho_id
                FETCH FIRST 1 ROWS ONLY
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, branchId);
            stmt.setString(2, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString("kho_id")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể xác định kho mặc định.", e);
        }
    }

    public List<InventoryItem> findMaterialsByWarehouse(String khoId) throws DatabaseException {
        String sql = """
                SELECT sp.san_pham_id, sp.ten_san_pham, sp.don_vi_tinh,
                       NVL(tk.so_luong_ton, 0) AS so_luong_ton,
                       NVL(tk.muc_ton_toi_thieu, 0) AS muc_ton_toi_thieu
                FROM san_pham sp
                LEFT JOIN ton_kho tk ON tk.san_pham_id = sp.san_pham_id AND tk.kho_id = ?
                WHERE sp.loai_san_pham = 'NGUYEN_LIEU'
                  AND sp.trang_thai = 1
                ORDER BY sp.ten_san_pham
                """;
        List<InventoryItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, khoId);
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
            throw new DatabaseException("Không thể tải danh sách nguyên liệu.", e);
        }
    }

    public void createSlip(WarehouseSlip slip) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                switch (slip.getSlipType()) {
                    case WarehouseSlipType.IMPORT -> insertImport(conn, slip);
                    case WarehouseSlipType.EXPORT -> insertExport(conn, slip);
                    case WarehouseSlipType.STOCKTAKE -> insertStocktake(conn, slip);
                    default -> throw new DatabaseException("Loại phiếu kho không hợp lệ.");
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Không thể tạo phiếu kho.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tạo phiếu kho.", e);
        }
    }

    public List<WarehouseApprovalItem> findApprovalItems(String branchId, String type, String status) throws DatabaseException {
        List<WarehouseApprovalItem> items = new ArrayList<>();
        if (type == null || WarehouseSlipType.IMPORT.equals(type)) {
            items.addAll(findImportApprovals(branchId));
        }
        if (type == null || WarehouseSlipType.EXPORT.equals(type)) {
            items.addAll(findExportApprovals(branchId));
        }
        if (type == null || WarehouseSlipType.STOCKTAKE.equals(type)) {
            items.addAll(findStocktakeApprovals(branchId));
        }
        items.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        if (status == null || status.isBlank()) {
            return items;
        }
        return items.stream().filter(item -> status.equals(item.getStatus())).toList();
    }

    public void approveSlip(String slipType, String slipId, String approvedBy) throws DatabaseException, ValidationException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                switch (slipType) {
                    case WarehouseSlipType.IMPORT -> approveImport(conn, slipId);
                    case WarehouseSlipType.EXPORT -> approveExport(conn, slipId);
                    case WarehouseSlipType.STOCKTAKE -> approveStocktake(conn, slipId);
                    default -> throw new ValidationException("Loại phiếu kho không hợp lệ.");
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Không thể duyệt phiếu kho.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể duyệt phiếu kho.", e);
        }
    }

    public void rejectSlip(String slipType, String slipId, String approvedBy, String rejectedReason) throws DatabaseException, ValidationException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                switch (slipType) {
                    case WarehouseSlipType.IMPORT -> reject(conn, "phieu_nhap_kho", "phieu_nhap_id", slipId, rejectedReason);
                    case WarehouseSlipType.EXPORT -> reject(conn, "phieu_xuat_kho", "phieu_xuat_id", slipId, rejectedReason);
                    case WarehouseSlipType.STOCKTAKE -> reject(conn, "kiem_ke_kho", "kiem_ke_id", slipId, rejectedReason);
                    default -> throw new ValidationException("Loại phiếu kho không hợp lệ.");
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Không thể từ chối phiếu kho.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Không thể từ chối phiếu kho.", e);
        }
    }

    private void insertImport(Connection conn, WarehouseSlip slip) throws SQLException {
        String sql = """
                INSERT INTO phieu_nhap_kho (
                    phieu_nhap_id, kho_id, nha_cung_cap_id, nhan_vien_id, ngay_nhap,
                    tong_so_luong, so_luong_mat_hang, trang_thai, ghi_chu
                ) VALUES (?, ?, ?, ?, SYSTIMESTAMP, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slip.getSlipId());
            stmt.setString(2, slip.getKhoId());
            stmt.setString(3, slip.getSupplierId());
            stmt.setString(4, slip.getCreatedBy());
            stmt.setBigDecimal(5, slip.getTotalQuantity());
            stmt.setInt(6, slip.getLines().size());
            stmt.setString(7, DB_STATUS_OPEN);
            stmt.setString(8, encodeNote(slip.getStatus(), slip.getNote()));
            stmt.executeUpdate();
        }
        insertImportLines(conn, slip);
    }

    private void insertExport(Connection conn, WarehouseSlip slip) throws SQLException {
        String sql = """
                INSERT INTO phieu_xuat_kho (
                    phieu_xuat_id, kho_id, nhan_vien_id, ngay_xuat, ly_do_xuat,
                    so_luong_mat_hang, tong_tien, trang_thai, ghi_chu
                ) VALUES (?, ?, ?, SYSTIMESTAMP, ?, ?, 0, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slip.getSlipId());
            stmt.setString(2, slip.getKhoId());
            stmt.setString(3, slip.getCreatedBy());
            stmt.setString(4, slip.getReason());
            stmt.setInt(5, slip.getLines().size());
            stmt.setString(6, DB_STATUS_OPEN);
            stmt.setString(7, encodeNote(slip.getStatus(), slip.getNote()));
            stmt.executeUpdate();
        }
        insertExportLines(conn, slip);
    }

    private void insertStocktake(Connection conn, WarehouseSlip slip) throws SQLException {
        String sql = """
                INSERT INTO kiem_ke_kho (
                    kiem_ke_id, kho_id, nhan_vien_id, ngay_kiem_ke, trang_thai, ghi_chu
                ) VALUES (?, ?, ?, SYSTIMESTAMP, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slip.getSlipId());
            stmt.setString(2, slip.getKhoId());
            stmt.setString(3, slip.getCreatedBy());
            stmt.setString(4, DB_STATUS_OPEN);
            stmt.setString(5, encodeNote(slip.getStatus(), slip.getNote()));
            stmt.executeUpdate();
        }
        String detailSql = """
                INSERT INTO chi_tiet_kiem_ke_kho (
                    chi_tiet_kiem_ke_id, kiem_ke_id, san_pham_id, so_luong_he_thong, so_luong_thuc_te
                ) VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(detailSql)) {
            int index = 1;
            for (WarehouseSlipLine line : slip.getLines()) {
                stmt.setString(1, slip.getSlipId() + "-D" + index);
                stmt.setString(2, slip.getSlipId());
                stmt.setString(3, line.getItemId());
                stmt.setBigDecimal(4, zeroIfNull(line.getSystemQuantity()));
                stmt.setBigDecimal(5, zeroIfNull(line.getActualQuantity()));
                stmt.addBatch();
                index++;
            }
            stmt.executeBatch();
        }
    }

    private void insertImportLines(Connection conn, WarehouseSlip slip) throws SQLException {
        String sql = """
                INSERT INTO chi_tiet_nhap_kho (chi_tiet_nhap_id, phieu_nhap_id, san_pham_id, so_luong, don_gia)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (WarehouseSlipLine line : slip.getLines()) {
                stmt.setString(1, slip.getSlipId() + "-D" + index);
                stmt.setString(2, slip.getSlipId());
                stmt.setString(3, line.getItemId());
                stmt.setBigDecimal(4, zeroIfNull(line.getQuantity()));
                stmt.setBigDecimal(5, zeroIfNull(line.getUnitPrice()));
                stmt.addBatch();
                index++;
            }
            stmt.executeBatch();
        }
    }

    private void insertExportLines(Connection conn, WarehouseSlip slip) throws SQLException {
        String sql = """
                INSERT INTO chi_tiet_xuat_kho (chi_tiet_xuat_id, phieu_xuat_id, san_pham_id, so_luong, don_gia)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (WarehouseSlipLine line : slip.getLines()) {
                stmt.setString(1, slip.getSlipId() + "-D" + index);
                stmt.setString(2, slip.getSlipId());
                stmt.setString(3, line.getItemId());
                stmt.setBigDecimal(4, zeroIfNull(line.getQuantity()));
                stmt.setBigDecimal(5, zeroIfNull(line.getUnitPrice()));
                stmt.addBatch();
                index++;
            }
            stmt.executeBatch();
        }
    }

    private List<WarehouseApprovalItem> findImportApprovals(String branchId) throws DatabaseException {
        String sql = """
                SELECT pnk.phieu_nhap_id, pnk.kho_id, pnk.nhan_vien_id, pnk.ngay_nhap,
                       pnk.so_luong_mat_hang, pnk.trang_thai, pnk.ghi_chu,
                       NVL(ncc.ten_nha_cung_cap, '') AS related_party
                FROM phieu_nhap_kho pnk
                JOIN kho k ON k.kho_id = pnk.kho_id
                LEFT JOIN nha_cung_cap ncc ON ncc.nha_cung_cap_id = pnk.nha_cung_cap_id
                WHERE (? IS NULL OR k.chi_nhanh_id = ?)
                  AND pnk.trang_thai = ?
                """;
        return queryApprovalItems(sql, branchId, WarehouseSlipType.IMPORT);
    }

    private List<WarehouseApprovalItem> findExportApprovals(String branchId) throws DatabaseException {
        String sql = """
                SELECT pxk.phieu_xuat_id, pxk.kho_id, pxk.nhan_vien_id, pxk.ngay_xuat,
                       pxk.so_luong_mat_hang, pxk.trang_thai, pxk.ghi_chu,
                       pxk.ly_do_xuat AS related_party
                FROM phieu_xuat_kho pxk
                JOIN kho k ON k.kho_id = pxk.kho_id
                WHERE (? IS NULL OR k.chi_nhanh_id = ?)
                  AND pxk.trang_thai = ?
                """;
        return queryApprovalItems(sql, branchId, WarehouseSlipType.EXPORT);
    }

    private List<WarehouseApprovalItem> findStocktakeApprovals(String branchId) throws DatabaseException {
        String sql = """
                SELECT kk.kiem_ke_id, kk.kho_id, kk.nhan_vien_id, kk.ngay_kiem_ke,
                       (SELECT COUNT(*) FROM chi_tiet_kiem_ke_kho ct WHERE ct.kiem_ke_id = kk.kiem_ke_id) AS so_luong_mat_hang,
                       kk.trang_thai, kk.ghi_chu,
                       kk.ghi_chu AS related_party
                FROM kiem_ke_kho kk
                JOIN kho k ON k.kho_id = kk.kho_id
                WHERE (? IS NULL OR k.chi_nhanh_id = ?)
                  AND kk.trang_thai = ?
                """;
        return queryApprovalItems(sql, branchId, WarehouseSlipType.STOCKTAKE);
    }

    private List<WarehouseApprovalItem> queryApprovalItems(String sql, String branchId, String type) throws DatabaseException {
        List<WarehouseApprovalItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, branchId);
            stmt.setString(2, branchId);
            stmt.setString(3, DB_STATUS_OPEN);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String note = rs.getString("ghi_chu");
                    if (isDraftNote(note)) {
                        continue;
                    }
                    WarehouseApprovalItem item = new WarehouseApprovalItem();
                    item.setSlipType(type);
                    item.setSlipId(rs.getString(1));
                    item.setKhoId(rs.getString("kho_id"));
                    item.setCreatedBy(rs.getString("nhan_vien_id"));
                    item.setRelatedParty(stripWorkflowPrefix(rs.getString("related_party")));
                    item.setReason(WarehouseSlipType.EXPORT.equals(type) ? stripWorkflowPrefix(rs.getString("related_party")) : null);
                    item.setStatus(WarehouseSlipStatus.PENDING_APPROVAL);
                    item.setRejectedReason(null);
                    item.setItemCount(rs.getInt("so_luong_mat_hang"));
                    item.setCreatedAt(rs.getTimestamp(4).toLocalDateTime());
                    items.add(item);
                }
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách phiếu chờ duyệt.", e);
        }
    }

    private void approveImport(Connection conn, String slipId) throws SQLException, ValidationException {
        String khoId = loadWarehouseId(conn, "phieu_nhap_kho", "phieu_nhap_id", slipId);
        lockPendingRow(conn, "phieu_nhap_kho", "phieu_nhap_id", slipId);
        for (WarehouseSlipLine line : loadImportLines(conn, slipId)) {
            increaseInventory(conn, khoId, line.getItemId(), line.getQuantity());
        }
        markApproved(conn, "phieu_nhap_kho", "phieu_nhap_id", slipId);
    }

    private void approveExport(Connection conn, String slipId) throws SQLException, ValidationException {
        String khoId = loadWarehouseId(conn, "phieu_xuat_kho", "phieu_xuat_id", slipId);
        lockPendingRow(conn, "phieu_xuat_kho", "phieu_xuat_id", slipId);
        for (WarehouseSlipLine line : loadExportLines(conn, slipId)) {
            BigDecimal currentQty = getCurrentInventoryForUpdate(conn, khoId, line.getItemId());
            if (currentQty.compareTo(zeroIfNull(line.getQuantity())) < 0) {
                throw new ValidationException("Phiếu đã vượt tồn kho khả dụng tại thời điểm duyệt.");
            }
            decreaseInventory(conn, khoId, line.getItemId(), line.getQuantity());
        }
        markApproved(conn, "phieu_xuat_kho", "phieu_xuat_id", slipId);
    }

    private void approveStocktake(Connection conn, String slipId) throws SQLException, ValidationException {
        String khoId = loadWarehouseId(conn, "kiem_ke_kho", "kiem_ke_id", slipId);
        lockPendingRow(conn, "kiem_ke_kho", "kiem_ke_id", slipId);
        for (WarehouseSlipLine line : loadStocktakeLines(conn, slipId)) {
            upsertInventory(conn, khoId, line.getItemId(), zeroIfNull(line.getActualQuantity()));
        }
        markApproved(conn, "kiem_ke_kho", "kiem_ke_id", slipId, DB_STATUS_STOCKTAKE_APPROVED);
    }

    private void reject(Connection conn, String tableName, String idColumn, String slipId, String rejectedReason) throws SQLException, ValidationException {
        lockPendingRow(conn, tableName, idColumn, slipId);
        String currentNote = loadNote(conn, tableName, idColumn, slipId);
        String sql = "UPDATE " + tableName + " SET trang_thai = ?, ghi_chu = ? WHERE " + idColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DB_STATUS_REJECTED);
            stmt.setString(2, appendRejectedReason(currentNote, rejectedReason));
            stmt.setString(3, slipId);
            stmt.executeUpdate();
        }
    }

    private void lockPendingRow(Connection conn, String tableName, String idColumn, String slipId) throws SQLException, ValidationException {
        String sql = "SELECT trang_thai, ghi_chu FROM " + tableName + " WHERE " + idColumn + " = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ValidationException("Không tìm thấy phiếu kho.");
                }
                if (!DB_STATUS_OPEN.equals(rs.getString("trang_thai")) || isDraftNote(rs.getString("ghi_chu"))) {
                    throw new ValidationException("Phiếu đã được người khác xử lý trước đó.");
                }
            }
        }
    }

    private void markApproved(Connection conn, String tableName, String idColumn, String slipId) throws SQLException {
        String sql = "UPDATE " + tableName + " SET trang_thai = ?, ghi_chu = ? WHERE " + idColumn + " = ?";
        String currentNote = loadNote(conn, tableName, idColumn, slipId);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DB_STATUS_APPROVED);
            stmt.setString(2, stripWorkflowPrefix(currentNote));
            stmt.setString(3, slipId);
            stmt.executeUpdate();
        }
    }

    private void markApproved(Connection conn, String tableName, String idColumn, String slipId, String approvedStatus) throws SQLException {
        String sql = "UPDATE " + tableName + " SET trang_thai = ?, ghi_chu = ? WHERE " + idColumn + " = ?";
        String currentNote = loadNote(conn, tableName, idColumn, slipId);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, approvedStatus);
            stmt.setString(2, stripWorkflowPrefix(currentNote));
            stmt.setString(3, slipId);
            stmt.executeUpdate();
        }
    }

    private String loadWarehouseId(Connection conn, String tableName, String idColumn, String slipId) throws SQLException {
        String sql = "SELECT kho_id FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("kho_id") : null;
            }
        }
    }

    private String loadNote(Connection conn, String tableName, String idColumn, String slipId) throws SQLException {
        String sql = "SELECT ghi_chu FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("ghi_chu") : null;
            }
        }
    }

    private List<WarehouseSlipLine> loadImportLines(Connection conn, String slipId) throws SQLException {
        return loadQuantityLines(conn, "SELECT san_pham_id, so_luong, don_gia FROM chi_tiet_nhap_kho WHERE phieu_nhap_id = ?", slipId);
    }

    private List<WarehouseSlipLine> loadExportLines(Connection conn, String slipId) throws SQLException {
        return loadQuantityLines(conn, "SELECT san_pham_id, so_luong, don_gia FROM chi_tiet_xuat_kho WHERE phieu_xuat_id = ?", slipId);
    }

    private List<WarehouseSlipLine> loadQuantityLines(Connection conn, String sql, String slipId) throws SQLException {
        List<WarehouseSlipLine> lines = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(new WarehouseSlipLine(
                            rs.getString("san_pham_id"),
                            null,
                            null,
                            null,
                            null,
                            rs.getBigDecimal("so_luong"),
                            rs.getBigDecimal("don_gia"),
                            null
                    ));
                }
            }
        }
        return lines;
    }

    private List<WarehouseSlipLine> loadStocktakeLines(Connection conn, String slipId) throws SQLException {
        String sql = """
                SELECT san_pham_id, so_luong_he_thong, so_luong_thuc_te
                FROM chi_tiet_kiem_ke_kho
                WHERE kiem_ke_id = ?
                """;
        List<WarehouseSlipLine> lines = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slipId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(new WarehouseSlipLine(
                            rs.getString("san_pham_id"),
                            null,
                            null,
                            rs.getBigDecimal("so_luong_he_thong"),
                            rs.getBigDecimal("so_luong_thuc_te"),
                            null,
                            null,
                            null
                    ));
                }
            }
        }
        return lines;
    }

    private BigDecimal getCurrentInventoryForUpdate(Connection conn, String khoId, String sanPhamId) throws SQLException {
        String sql = "SELECT so_luong_ton FROM ton_kho WHERE kho_id = ? AND san_pham_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, khoId);
            stmt.setString(2, sanPhamId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("so_luong_ton") : BigDecimal.ZERO;
            }
        }
    }

    private void increaseInventory(Connection conn, String khoId, String sanPhamId, BigDecimal quantity) throws SQLException {
        BigDecimal currentQty = getCurrentInventoryForUpdate(conn, khoId, sanPhamId);
        upsertInventory(conn, khoId, sanPhamId, currentQty.add(zeroIfNull(quantity)));
    }

    private void decreaseInventory(Connection conn, String khoId, String sanPhamId, BigDecimal quantity) throws SQLException {
        BigDecimal currentQty = getCurrentInventoryForUpdate(conn, khoId, sanPhamId);
        upsertInventory(conn, khoId, sanPhamId, currentQty.subtract(zeroIfNull(quantity)));
    }

    private void upsertInventory(Connection conn, String khoId, String sanPhamId, BigDecimal newQuantity) throws SQLException {
        String updateSql = "UPDATE ton_kho SET so_luong_ton = ?, last_updated = SYSTIMESTAMP WHERE kho_id = ? AND san_pham_id = ?";
        try (PreparedStatement update = conn.prepareStatement(updateSql)) {
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

    private String encodeNote(String logicalStatus, String note) {
        String prefix = WarehouseSlipStatus.DRAFT.equals(logicalStatus) ? NOTE_DRAFT_PREFIX : NOTE_PENDING_PREFIX;
        String cleanNote = note == null ? "" : note.trim();
        return cleanNote.isBlank() ? prefix : prefix + "\n" + cleanNote;
    }

    private boolean isDraftNote(String note) {
        return note != null && note.startsWith(NOTE_DRAFT_PREFIX);
    }

    private String stripWorkflowPrefix(String note) {
        if (note == null || note.isBlank()) {
            return note;
        }
        if (note.startsWith(NOTE_DRAFT_PREFIX)) {
            return note.substring(NOTE_DRAFT_PREFIX.length()).stripLeading();
        }
        if (note.startsWith(NOTE_PENDING_PREFIX)) {
            return note.substring(NOTE_PENDING_PREFIX.length()).stripLeading();
        }
        if (note.startsWith(NOTE_PENDING_PREFIX_LEGACY)) {
            return note.substring(NOTE_PENDING_PREFIX_LEGACY.length()).stripLeading();
        }
        return note;
    }

    private String appendRejectedReason(String note, String rejectedReason) {
        String cleanNote = stripWorkflowPrefix(note);
        String reasonLine = "Lý do từ chối: " + rejectedReason;
        if (cleanNote == null || cleanNote.isBlank()) {
            return reasonLine;
        }
        return cleanNote + "\n" + reasonLine;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
