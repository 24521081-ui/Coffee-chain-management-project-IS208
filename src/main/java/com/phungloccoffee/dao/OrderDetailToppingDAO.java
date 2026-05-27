package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.OrderDetailTopping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OrderDetailToppingDAO {
    public void insert(Connection conn, OrderDetailTopping topping) throws DatabaseException {
        String sql = """
                INSERT INTO order_detail_topping
                    (id, order_detail_id, topping_product_id, topping_name, quantity, unit_price, subtotal, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, topping.getId());
            stmt.setString(2, topping.getOrderDetailId());
            stmt.setString(3, topping.getToppingProductId());
            stmt.setString(4, topping.getToppingName());
            stmt.setBigDecimal(5, topping.getQuantity());
            stmt.setBigDecimal(6, topping.getUnitPrice());
            stmt.setBigDecimal(7, topping.getSubtotal());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Không thể lưu topping của chi tiết đơn hàng: " + e.getMessage(), e);
        }
    }
}
