package com.phungloccoffee.bus;

import com.phungloccoffee.dao.CustomerDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.CustomerPageResult;
import com.phungloccoffee.model.CustomerSummary;
import com.phungloccoffee.model.KhachHang;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class CustomerBUS extends PermissionBUS {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile("^(0(3|5|7|8|9)[0-9]{8}|84(3|5|7|8|9)[0-9]{8}|\\+84(3|5|7|8|9)[0-9]{8})$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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

    public KhachHang validateAndBuildNewMember(String name, String phone, String email)
            throws DatabaseException, ValidationException {
        String cleanName = normalize(name);
        String cleanPhone = normalizePhone(phone);
        String cleanEmail = normalize(email);

        if (cleanName == null) {
            throw new ValidationException("Vui lòng nhập tên khách hàng");
        }
        if (cleanPhone == null) {
            throw new ValidationException("Vui lòng nhập số điện thoại");
        }
        if (!VIETNAM_PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new ValidationException("Số điện thoại không hợp lệ");
        }
        cleanPhone = toLocalPhone(cleanPhone);
        if (cleanEmail != null && !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ValidationException("Email không hợp lệ");
        }
        if (customerDAO.existsByPhone(cleanPhone)) {
            throw new ValidationException("Số điện thoại này đã tồn tại trong hệ thống");
        }
        if (cleanEmail != null && customerDAO.existsByEmail(cleanEmail)) {
            throw new ValidationException("Email này đã tồn tại trong hệ thống");
        }

        KhachHang customer = new KhachHang();
        customer.setKhachHangId(generateCustomerId());
        customer.setHoTen(cleanName);
        customer.setPhone(cleanPhone);
        customer.setEmail(cleanEmail);
        customer.setTrangThai(1);
        return customer;
    }

    public void createCustomer(Connection conn, KhachHang customer) throws DatabaseException {
        customerDAO.insert(conn, customer);
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

    private String normalizePhone(String phone) {
        String normalized = normalize(phone);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("[\\s.-]", "");
    }

    private String toLocalPhone(String phone) {
        if (phone.startsWith("+84")) {
            return "0" + phone.substring(3);
        }
        if (phone.startsWith("84")) {
            return "0" + phone.substring(2);
        }
        return phone;
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

    private String generateCustomerId() {
        return "KH" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
