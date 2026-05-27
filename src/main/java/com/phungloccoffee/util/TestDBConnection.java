package com.phungloccoffee.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestDBConnection {

    public static void main(String[] args) {
        System.out.println("Dang test ket noi Oracle...");
        DBConnection.printDebugConfig();

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("Ket noi Oracle thanh cong!");
            System.out.println("Database: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Database version: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("User from metadata: " + conn.getMetaData().getUserName());

            printSingleValue(conn, "SELECT USER FROM dual", "Current schema/user");
            printSingleValue(conn, "SELECT SYS_CONTEXT('USERENV', 'CON_NAME') FROM dual", "Container/PDB");
            printSingleValue(conn, "SELECT SYS_CONTEXT('USERENV', 'SERVICE_NAME') FROM dual", "Service name");
            printSingleValue(conn, "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM dual", "Current schema");
            printSingleValue(conn, "SELECT COUNT(*) FROM app_user", "app_user row count");
            printSingleValue(
                    conn,
                    "SELECT COUNT(*) FROM app_user WHERE TRIM(LOWER(ten_dang_nhap)) = 'admin'",
                    "admin account count"
            );
            printTableOwners(conn);
            printAdminRows(conn);

        } catch (Exception e) {
            System.out.println("Ket noi Oracle that bai!");
            System.out.println("Loai loi: " + e.getClass().getName());
            System.out.println("Thong bao loi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printSingleValue(Connection conn, String sql, String label) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                System.out.println(label + ": " + rs.getString(1));
            }
        }
    }

    private static void printTableOwners(Connection conn) throws Exception {
        String sql = """
                SELECT owner, table_name
                FROM all_tables
                WHERE table_name IN ('APP_USER', 'NHAN_VIEN', 'CHI_NHANH')
                ORDER BY owner, table_name
                """;
        System.out.println("Tables visible to Java connection:");
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("- " + rs.getString("owner") + "." + rs.getString("table_name"));
            }
            if (!found) {
                System.out.println("- No APP_USER/NHAN_VIEN/CHI_NHANH tables visible.");
            }
        }
    }

    private static void printAdminRows(Connection conn) throws Exception {
        String sql = """
                SELECT user_id, ten_dang_nhap, vai_tro, trang_thai
                FROM app_user
                WHERE TRIM(LOWER(ten_dang_nhap)) = 'admin'
                """;
        System.out.println("Admin rows visible in PL_COFFEE.app_user:");
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("- " + rs.getString("user_id")
                        + " | " + rs.getString("ten_dang_nhap")
                        + " | " + rs.getString("vai_tro")
                        + " | " + rs.getInt("trang_thai"));
            }
            if (!found) {
                System.out.println("- No admin row visible.");
            }
        }
    }
}
