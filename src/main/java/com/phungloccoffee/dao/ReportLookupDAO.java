package com.phungloccoffee.dao;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.phungloccoffee.model.report.ReportModels.ALL_BRANCH_ID;
import static com.phungloccoffee.model.report.ReportModels.ALL_CATEGORY;

public class ReportLookupDAO {
    public List<BranchOption> findBranchOptions() throws DatabaseException {
        String sql = """
                SELECT chi_nhanh_id, ten_chi_nhanh
                FROM chi_nhanh
                WHERE trang_thai = 1
                ORDER BY ten_chi_nhanh
                """;
        List<BranchOption> branches = new ArrayList<>();
        branches.add(new BranchOption(ALL_BRANCH_ID, "Tất cả chi nhánh", "Tất cả", "Tất cả chi nhánh"));
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("chi_nhanh_id");
                String fullName = rs.getString("ten_chi_nhanh");
                String displayName = normalizeBranchName(fullName);
                branches.add(new BranchOption(id, displayName, chartLabel(displayName), fullName));
            }
            return branches;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải danh sách chi nhánh.", e);
        }
    }

    public List<String> findProductCategories() throws DatabaseException {
        String sql = """
                SELECT ten_danh_muc
                FROM danh_muc_san_pham
                ORDER BY ten_danh_muc
                """;
        List<String> categories = new ArrayList<>();
        categories.add(ALL_CATEGORY);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("ten_danh_muc"));
            }
            return categories;
        } catch (SQLException e) {
            throw new DatabaseException("Không thể tải nhóm sản phẩm.", e);
        }
    }

    public static String normalizeBranchName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName
                .replaceFirst("(?iu)^\\s*chi\\s+nh[aá]nh\\s+", "")
                .trim();
    }

    public static String chartLabel(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace < 0 || trimmed.length() <= 8) {
            return trimmed;
        }
        return trimmed.substring(0, firstSpace) + "\n" + trimmed.substring(firstSpace + 1);
    }
}
