package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO {
    private static final String PAID_STATUS = "DA_THANH_TOAN";
    private static final String COMPLETED_STATUS = "DA_HOAN_THANH";

    public void createOrder(Order order, List<OrderDetail> details) throws DatabaseException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            insert(conn, order);
            ChiTietDonHangDAO detailDAO = new ChiTietDonHangDAO();
            OrderDetailToppingDAO toppingDAO = new OrderDetailToppingDAO();
            for (OrderDetail detail : details) {
                detail.setDonHangId(order.getDonHangId());
                detailDAO.insert(conn, detail);
                for (var topping : detail.getToppings()) {
                    topping.setOrderDetailId(detail.getChiTietDonHangId());
                    toppingDAO.insert(conn, topping);
                }
            }
            conn.commit();
        } catch (Exception e) {
            rollbackQuietly(conn);
            if (e instanceof DatabaseException databaseException) {
                throw databaseException;
            }
            throw new DatabaseException("Khong the tao hoa don.", e);
        } finally {
            closeQuietly(conn);
        }
    }

    public void syncOfflineOrder(Order order, List<OrderDetail> details, boolean deductInventory)
            throws DatabaseException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Optional<Order> existingOrder = findOrderById(conn, order.getDonHangId());
            if (existingOrder.isPresent()) {
                Order existing = existingOrder.get();
                if (deductInventory && !PAID_STATUS.equals(existing.getTrangThaiThanhToan())) {
                    List<OrderDetail> existingDetails = new ChiTietDonHangDAO().findByDonHangId(conn, order.getDonHangId());
                    deductInventoryForPayment(conn, existing, existingDetails.isEmpty() ? details : existingDetails);
                    completePaidOrder(conn, order.getDonHangId());
                }
                conn.commit();
                return;
            }

            insert(conn, order);
            ChiTietDonHangDAO detailDAO = new ChiTietDonHangDAO();
            OrderDetailToppingDAO toppingDAO = new OrderDetailToppingDAO();
            for (OrderDetail detail : details) {
                detail.setDonHangId(order.getDonHangId());
                detailDAO.insert(conn, detail);
                for (var topping : detail.getToppings()) {
                    topping.setOrderDetailId(detail.getChiTietDonHangId());
                    toppingDAO.insert(conn, topping);
                }
            }
            if (deductInventory) {
                deductInventoryForPayment(conn, order, details);
            }
            conn.commit();
        } catch (Exception e) {
            rollbackQuietly(conn);
            if (e instanceof DatabaseException databaseException) {
                throw databaseException;
            }
            throw new DatabaseException("Khong the dong bo don offline: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    public void insert(Order order) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection()) {
            insert(conn, order);
        } catch (SQLException e) {
            throw new DatabaseException("Khong the tao don hang: " + e.getMessage(), e);
        }
    }

    public void insert(Connection conn, Order order) throws DatabaseException {
        String sql = """
                INSERT INTO don_hang (don_hang_id, khach_hang_id, chi_nhanh_id, nhan_vien_id, trang_thai,
                                      tam_tinh, giam_gia, tong_tien, trang_thai_thanh_toan, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, order.getDonHangId());
            stmt.setString(2, order.getKhachHangId());
            stmt.setString(3, order.getChiNhanhId());
            stmt.setString(4, order.getNhanVienId());
            stmt.setString(5, order.getTrangThai());
            stmt.setBigDecimal(6, order.getTamTinh());
            stmt.setBigDecimal(7, order.getGiamGia());
            stmt.setBigDecimal(8, order.getTongTien());
            stmt.setString(9, order.getTrangThaiThanhToan());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Khong the tao don hang: " + e.getMessage(), e);
        }
    }

    public Optional<Order> findById(String donHangId) throws DatabaseException {
        String sql = "SELECT * FROM don_hang WHERE don_hang_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, donHangId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Khong the tai don hang.", e);
        }
    }

    public Order confirmPayment(String donHangId) throws DatabaseException, ValidationException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Order order = confirmPayment(conn, donHangId);
            conn.commit();
            return order;
        } catch (ValidationException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Khong the xac nhan thanh toan: " + e.getMessage(), e);
        } catch (DatabaseException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    public Order confirmPayment(Connection conn, String donHangId) throws DatabaseException, ValidationException {
        Order order = findOrderByIdForUpdate(conn, donHangId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn hàng: " + donHangId));
        if (PAID_STATUS.equals(order.getTrangThaiThanhToan())) {
            throw new ValidationException("Đơn hàng này đã được thanh toán");
        }

        List<OrderDetail> details = new ChiTietDonHangDAO().findByDonHangId(conn, donHangId);
        deductInventoryForPayment(conn, order, details);
        completePaidOrder(conn, donHangId);

        order.setTrangThai(COMPLETED_STATUS);
        order.setTrangThaiThanhToan(PAID_STATUS);
        return order;
    }

    public Order confirmPaymentAndAssignCustomer(Connection conn, String donHangId, String khachHangId)
            throws DatabaseException, ValidationException {
        Order order = findOrderByIdForUpdate(conn, donHangId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn hàng: " + donHangId));
        if (PAID_STATUS.equals(order.getTrangThaiThanhToan())) {
            throw new ValidationException("Đơn hàng này đã được thanh toán");
        }
        if (order.getKhachHangId() != null && !order.getKhachHangId().isBlank()
                && !order.getKhachHangId().equals(khachHangId)) {
            throw new ValidationException("Đơn hàng này đã có khách hàng.");
        }

        List<OrderDetail> details = new ChiTietDonHangDAO().findByDonHangId(conn, donHangId);
        deductInventoryForPayment(conn, order, details);
        completePaidOrder(conn, donHangId, khachHangId);

        order.setKhachHangId(khachHangId);
        order.setTrangThai(COMPLETED_STATUS);
        order.setTrangThaiThanhToan(PAID_STATUS);
        return order;
    }

    private void deductInventoryForPayment(Connection conn, Order order, List<OrderDetail> details) throws DatabaseException {
        if (order.getChiNhanhId() == null || order.getChiNhanhId().isBlank()) {
            throw new DatabaseException("Đơn hàng chưa có chi nhánh nên không thể trừ tồn kho.");
        }
        if (details == null || details.isEmpty()) {
            throw new DatabaseException("Đơn hàng chưa có chi tiết nên không thể trừ tồn kho.");
        }

        InventoryDAO inventoryDAO = new InventoryDAO();
        String khoId = inventoryDAO.findActiveKhoIdByBranch(conn, order.getChiNhanhId())
                .orElseThrow(() -> new DatabaseException("Không tìm thấy kho đang hoạt động của chi nhánh " + order.getChiNhanhId() + "."));

        for (OrderDetail detail : details) {
            if (detail.getSoLuong() == null || detail.getSoLuong().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DatabaseException("Số lượng món trong đơn không hợp lệ: " + detail.getSanPhamId());
            }

            List<InventoryDAO.StockDeduction> recipeDeductions = inventoryDAO.findRecipeDeductions(conn, detail.getSanPhamId());
            if (recipeDeductions.isEmpty()) {
                inventoryDAO.deductStock(conn, khoId, detail.getSanPhamId(), detail.getSoLuong());
                continue;
            }

            for (InventoryDAO.StockDeduction deduction : recipeDeductions) {
                BigDecimal requiredQuantity = deduction.getQuantity().multiply(detail.getSoLuong());
                inventoryDAO.deductStock(conn, khoId, deduction.getSanPhamId(), requiredQuantity);
            }
        }
    }

    public Optional<Order> findOrderByIdForUpdate(Connection conn, String donHangId) throws DatabaseException {
        String sql = "SELECT * FROM don_hang WHERE don_hang_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, donHangId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Khong the khoa don hang de thanh toan: " + e.getMessage(), e);
        }
    }

    private Optional<Order> findOrderById(Connection conn, String donHangId) throws DatabaseException {
        String sql = "SELECT * FROM don_hang WHERE don_hang_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, donHangId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Khong the kiem tra don hang: " + e.getMessage(), e);
        }
    }

    public void updatePaymentStatus(String donHangId, String status) throws DatabaseException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Order order = findOrderByIdForUpdate(conn, donHangId)
                    .orElseThrow(() -> new DatabaseException("Không tìm thấy đơn hàng: " + donHangId));
            if (PAID_STATUS.equals(status) && PAID_STATUS.equals(order.getTrangThaiThanhToan())) {
                throw new DatabaseException("Đơn hàng này đã được thanh toán");
            }

            updatePaymentStatus(conn, donHangId, status);
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Khong the cap nhat thanh toan don hang: " + e.getMessage(), e);
        } catch (DatabaseException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    private void updatePaymentStatus(Connection conn, String donHangId, String status) throws DatabaseException {
        String sql = "UPDATE don_hang SET trang_thai_thanh_toan = ?, updated_at = SYSTIMESTAMP WHERE don_hang_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, donHangId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Khong tim thay don hang de cap nhat thanh toan: " + donHangId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Khong the cap nhat thanh toan don hang: " + e.getMessage(), e);
        }
    }

    private void completePaidOrder(Connection conn, String donHangId) throws DatabaseException {
        String sql = """
                UPDATE don_hang
                SET trang_thai = ?, trang_thai_thanh_toan = ?, updated_at = SYSTIMESTAMP
                WHERE don_hang_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, COMPLETED_STATUS);
            stmt.setString(2, PAID_STATUS);
            stmt.setString(3, donHangId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Khong tim thay don hang de hoan thanh: " + donHangId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Khong the cap nhat don hang da hoan thanh: " + e.getMessage(), e);
        }
    }

    private void completePaidOrder(Connection conn, String donHangId, String khachHangId) throws DatabaseException {
        String sql = """
                UPDATE don_hang
                SET khach_hang_id = ?, trang_thai = ?, trang_thai_thanh_toan = ?, updated_at = SYSTIMESTAMP
                WHERE don_hang_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, khachHangId);
            stmt.setString(2, COMPLETED_STATUS);
            stmt.setString(3, PAID_STATUS);
            stmt.setString(4, donHangId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Khong tim thay don hang de hoan thanh: " + donHangId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Khong the cap nhat khach hang cho don hang: " + e.getMessage(), e);
        }
    }

    public List<Order> findRecentOrders() throws DatabaseException {
        String sql = "SELECT * FROM don_hang ORDER BY created_at DESC FETCH FIRST 50 ROWS ONLY";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                orders.add(map(rs));
            }
            return orders;
        } catch (SQLException e) {
            throw new DatabaseException("Khong the tai don hang gan day.", e);
        }
    }

    private Order map(ResultSet rs) throws SQLException {
        return new Order(
                rs.getString("don_hang_id"),
                rs.getString("khach_hang_id"),
                rs.getString("chi_nhanh_id"),
                rs.getString("nhan_vien_id"),
                rs.getString("trang_thai"),
                rs.getBigDecimal("tam_tinh"),
                rs.getBigDecimal("giam_gia"),
                rs.getBigDecimal("tong_tien"),
                rs.getString("trang_thai_thanh_toan"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException ignored) {
        }
    }
}
