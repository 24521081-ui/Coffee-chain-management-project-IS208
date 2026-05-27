package com.phungloccoffee.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "1521";
    private static final String DEFAULT_SERVICE = "freepdb1";

    private static final Properties CONFIG = loadConfig();
    private static final String URL = resolveUrl();
    private static final String USERNAME = readConfig("db.username", "DB_USERNAME");
    private static final String PASSWORD = readConfig("db.password", "DB_PASSWORD");

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (isBlank(USERNAME)) {
            throw new SQLException("Thiáº¿u database username. Vui lÃ²ng nháº­p db.username trong config.properties.");
        }
        if (isBlank(PASSWORD)) {
            throw new SQLException("Thiáº¿u database password. Vui lÃ²ng nháº­p db.password trong config.properties.");
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static boolean testConnection() {
        printDebugConfig();
        try (Connection conn = getConnection()) {
            System.out.println("Ket noi Oracle thanh cong: " + URL);
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Ket noi Oracle that bai: " + e.getMessage());
            return false;
        }
    }

    public static void printDebugConfig() {
        System.out.println("DB URL = " + URL);
        System.out.println("DB USERNAME = " + (isBlank(USERNAME) ? "<missing>" : USERNAME));
        System.out.println("DB PASSWORD SET = " + !isBlank(PASSWORD));
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        Path externalConfig = Path.of("config.properties");
        if (Files.exists(externalConfig)) {
            try (InputStream input = Files.newInputStream(externalConfig)) {
                properties.load(input);
                return properties;
            } catch (IOException e) {
                System.err.println("Khong the doc config.properties: " + e.getMessage());
            }
        }

        try (InputStream input = DBConnection.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Khong the doc config.properties trong resources: " + e.getMessage());
        }
        return properties;
    }

    private static String resolveUrl() {
        String configuredUrl = readConfig("db.url", "DB_URL");
        if (!isBlank(configuredUrl)) {
            return configuredUrl;
        }

        String host = readConfig("db.host", "DB_HOST", DEFAULT_HOST);
        String port = readConfig("db.port", "DB_PORT", DEFAULT_PORT);
        String service = readConfig("db.service", "DB_SERVICE", DEFAULT_SERVICE);
        return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;
    }

    private static String readConfig(String key, String envKey) {
        return readConfig(key, envKey, "");
    }

    private static String readConfig(String key, String envKey, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (!isBlank(systemValue)) {
            return systemValue.trim();
        }
        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }
        String configValue = CONFIG.getProperty(key);
        return isBlank(configValue) ? defaultValue : configValue.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
