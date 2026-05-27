package com.phungloccoffee.bus;

import com.phungloccoffee.dao.CustomerDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.CustomerPageResult;
import com.phungloccoffee.model.CustomerSummary;
import com.phungloccoffee.model.KhachHang;

import java.util.List;

public class CustomerBUS extends PermissionBUS {
    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerDAO customerDAO = new CustomerDAO();

    public CustomerSummary getCustomerSummary() throws DatabaseException, PermissionException {
        requireCustomerAccess();
        return new CustomerSummary(
                customerDAO.countAllCustomers(),
                customerDAO.countNewCustomersThisMonth()
        );
    }

    public CustomerPageResult getCustomerPage(String keyword, String rank, int currentPage, int pageSize)
            throws DatabaseException, PermissionException, ValidationException {
        requireCustomerAccess();
        String normalizedKeyword = normalize(keyword);
        String normalizedRank = normalizeRank(rank);
        int safePageSize = normalizePageSize(pageSize);
        int requestedPage = Math.max(1, currentPage);

        int totalRows = customerDAO.countCustomersByFilter(normalizedKeyword, normalizedRank);
        int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) safePageSize));
        int effectivePage = Math.min(requestedPage, totalPages);
        int offset = (effectivePage - 1) * safePageSize;

        List<KhachHang> customers = totalRows == 0
                ? List.of()
                : customerDAO.findCustomers(normalizedKeyword, normalizedRank, offset, safePageSize);
        return new CustomerPageResult(customers, effectivePage, safePageSize, totalRows, totalPages);
    }

    public List<String> getMembershipRanks() throws DatabaseException, PermissionException {
        requireCustomerAccess();
        return customerDAO.findMembershipRanks();
    }

    private void requireCustomerAccess() throws PermissionException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH", "BAN_GIAM_DOC", "IT_ADMIN");
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRank(String rank) {
        String normalized = normalize(rank);
        if (normalized == null || "Tất cả hạng".equalsIgnoreCase(normalized) || "Tat ca hang".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private int normalizePageSize(int pageSize) throws ValidationException {
        if (pageSize <= 0) {
            throw new ValidationException("Kích thước trang phải lớn hơn 0.");
        }
        if (pageSize > MAX_PAGE_SIZE) {
            throw new ValidationException("Kích thước trang không được vượt quá " + MAX_PAGE_SIZE + ".");
        }
        return pageSize;
    }
}
