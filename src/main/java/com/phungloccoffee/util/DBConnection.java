package com.phungloccoffee.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:oracle:thin:@//localhost:1521/freepdb1";
    private static final String USERNAME = "coffee_app ";
    private static final String PASSWORD = "123456";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Ket noi Oracle that bai: " + e.getMessage());
            return false;
        }
    }
}
