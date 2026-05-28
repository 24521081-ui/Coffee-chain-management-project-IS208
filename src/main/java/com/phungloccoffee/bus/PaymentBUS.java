package com.phungloccoffee.bus;

import com.phungloccoffee.dao.ChiTietDonHangDAO;
import com.phungloccoffee.dao.OrderDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.KhachHang;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.model.offline.OfflineInventoryMovement;
import com.phungloccoffee.model.offline.OfflineOrder;
import com.phungloccoffee.model.offline.OfflineOrderDetail;
import com.phungloccoffee.offline.InventoryCache;
import com.phungloccoffee.offline.NetworkMonitor;
import com.phungloccoffee.offline.OfflineStorage;
import com.phungloccoffee.offline.SyncService;
import com.phungloccoffee.util.DBConnection;
import com.phungloccoffee.util.ValidationUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PaymentBUS extends PermissionBUS {
    private static final String PAID_STATUS = "DA_THANH_TOAN";
    private static final String COMPLETED_STATUS = "DA_HOAN_THANH";
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of(
            "Tien mat",
            "Chuyen khoan",
            "Vi dien tu",
            "The ngan hang",
            "Ti\u1ec1n m\u1eb7t",
            "Chuy\u1ec3n kho\u1ea3n",
            "V\u00ed \u0111i\u1ec7n t\u1eed",
            "Th\u1ebb ng\u00e2n h\u00e0ng"
    );

    private final OrderDAO orderDAO = new OrderDAO();
    private final ChiTietDonHangDAO detailDAO = new ChiTietDonHangDAO();
    private final OfflineStorage offlineStorage = OfflineStorage.getInstance();
    private final InventoryCache inventoryCache = InventoryCache.getInstance();
    private final NetworkMonitor networkMonitor = NetworkMonitor.getInstance();
    private final SyncService syncService = new SyncService();
    private final CustomerBUS customerBUS = new CustomerBUS();

    public void pay(int orderId, String method, BigDecimal amount)
            throws ValidationException, PermissionException, DatabaseException {
        pay(String.valueOf(orderId), method, amount);
    }

    public void pay(String orderId, String method, BigDecimal amount)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH");
        ValidationUtils.requireText(orderId, "Mã hóa đơn");
        ValidationUtils.requireText(method, "Phương thức thanh toán");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Số tiền thanh toán phải lớn hơn 0.");
        }

        Order order = getOrderForPayment(orderId);
        if (PAID_STATUS.equals(order.getTrangThaiThanhToan())) {
            throw new ValidationException("Đơn hàng này đã được thanh toán");
        }
        if (amount.compareTo(order.getTongTien()) < 0) {
            throw new ValidationException("Số tiền thanh toán phải lớn hơn hoặc bằng tổng tiền.");
        }

        confirmPayment(orderId, method);
    }

    public Order confirmPayment(String orderId, String paymentMethod)
            throws ValidationException, PermissionException, DatabaseException {
        return confirmPaymentWithOptionalCustomer(orderId, paymentMethod, false, "", "", "").order();
    }

    public PaymentResult confirmPaymentWithOptionalCustomer(String orderId, String paymentMethod, boolean registerCustomer,
                                                            String customerName, String customerPhone, String customerEmail)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH");
        ValidationUtils.requireText(orderId, "Mã hóa đơn");
        ValidationUtils.requireText(paymentMethod, "Phương thức thanh toán");
        if (!ALLOWED_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new ValidationException("Phương thức thanh toán không hợp lệ.");
        }

        KhachHang newCustomer = null;
        if (registerCustomer) {
            newCustomer = customerBUS.validateAndBuildNewMember(customerName, customerPhone, customerEmail);
        }

        Optional<OfflineOrder> offlineOrder = findOfflineOrder(orderId);
        if (offlineOrder.isPresent()) {
            if (newCustomer != null) {
                throw new ValidationException("Không thể đăng ký khách hàng thành viên khi hóa đơn đang lưu offline.");
            }
            return new PaymentResult(confirmOfflinePayment(offlineOrder.get(), paymentMethod), false);
        }
        if (!networkMonitor.checkNow()) {
            throw new ValidationException("Đơn này chưa có trong lưu trữ offline nên không thể xác nhận khi mất kết nối Oracle.");
        }

        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn hàng: " + orderId));
        if (PAID_STATUS.equals(order.getTrangThaiThanhToan())) {
            throw new ValidationException("Đơn hàng này đã được thanh toán");
        }

        Order paidOrder = newCustomer == null
                ? orderDAO.confirmPayment(orderId)
                : confirmPaymentAndCreateCustomer(orderId, newCustomer);
        try {
            syncService.refreshInventoryCache(paidOrder.getChiNhanhId());
        } catch (Exception ignored) {
        }
        return new PaymentResult(paidOrder, newCustomer != null);
    }

    public Order getOrderForPayment(String orderId)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(orderId, "Mã hóa đơn");
        Optional<OfflineOrder> offlineOrder = findOfflineOrder(orderId);
        if (offlineOrder.isPresent()) {
            return toOrder(offlineOrder.get());
        }
        return orderDAO.findById(orderId)
                .orElseThrow(() -> new DatabaseException("Không tìm thấy đơn hàng: " + orderId));
    }

    public List<OrderDetail> getOrderDetailsForPayment(String orderId)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(orderId, "Mã hóa đơn");
        Optional<OfflineOrder> offlineOrder = findOfflineOrder(orderId);
        if (offlineOrder.isPresent()) {
            return toOrderDetails(offlineOrder.get());
        }
        return detailDAO.findByDonHangId(orderId);
    }

    private Order confirmPaymentAndCreateCustomer(String orderId, KhachHang customer)
            throws DatabaseException, ValidationException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            customerBUS.createCustomer(conn, customer);
            Order paidOrder = orderDAO.confirmPaymentAndAssignCustomer(conn, orderId, customer.getKhachHangId());
            conn.commit();
            return paidOrder;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Không thể thêm khách hàng. Vui lòng kiểm tra lại thông tin.", e);
        } catch (DatabaseException | ValidationException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    private Order confirmOfflinePayment(OfflineOrder offlineOrder, String paymentMethod)
            throws ValidationException, DatabaseException {
        if (PAID_STATUS.equals(offlineOrder.getPaymentStatus())) {
            throw new ValidationException("Đơn hàng này đã được thanh toán");
        }
        Map<String, BigDecimal> requiredQuantities = inventoryCache.calculateRequiredQuantities(offlineOrder.getDetails());
        List<String> insufficientItems = inventoryCache.findMissingOrInsufficientItems(requiredQuantities);
        if (!insufficientItems.isEmpty()) {
            throw new ValidationException("Tồn kho cache không đủ: " + String.join("; ", insufficientItems));
        }

        List<OfflineInventoryMovement> movements = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : requiredQuantities.entrySet()) {
            movements.add(new OfflineInventoryMovement(
                    offlineOrder.getLocalOrderId(),
                    entry.getKey(),
                    entry.getValue().negate(),
                    "SALE_DEDUCT",
                    OfflineOrder.SYNC_PENDING,
                    LocalDateTime.now()
            ));
        }

        inventoryCache.decrease(requiredQuantities);
        offlineOrder.setPaymentMethod(paymentMethod);
        offlineOrder.setPaymentStatus(PAID_STATUS);
        offlineOrder.setOrderStatus(COMPLETED_STATUS);
        offlineOrder.setInventoryMovements(movements);
        offlineOrder.setSyncStatus(OfflineOrder.SYNC_PENDING);
        offlineOrder.setLastError(null);
        offlineOrder.setUpdatedAt(LocalDateTime.now());
        try {
            offlineStorage.updateOrder(offlineOrder);
        } catch (IOException e) {
            inventoryCache.restore(requiredQuantities);
            throw new DatabaseException("Không thể lưu thanh toán offline: " + e.getMessage(), e);
        }
        return toOrder(offlineOrder);
    }

    private Optional<OfflineOrder> findOfflineOrder(String orderId) throws DatabaseException {
        try {
            return offlineStorage.findByLocalOrderId(orderId);
        } catch (IOException e) {
            throw new DatabaseException("Không thể đọc đơn offline: " + e.getMessage(), e);
        }
    }

    private Order toOrder(OfflineOrder offlineOrder) {
        return new Order(
                offlineOrder.getLocalOrderId(),
                null,
                offlineOrder.getBranchId(),
                offlineOrder.getCashierId(),
                offlineOrder.getOrderStatus(),
                offlineOrder.getTotalAmount(),
                BigDecimal.ZERO,
                offlineOrder.getTotalAmount(),
                offlineOrder.getPaymentStatus(),
                offlineOrder.getCreatedAt(),
                offlineOrder.getUpdatedAt()
        );
    }

    private List<OrderDetail> toOrderDetails(OfflineOrder offlineOrder) {
        List<OrderDetail> details = new ArrayList<>();
        for (OfflineOrderDetail detail : offlineOrder.getDetails()) {
            details.add(new OrderDetail(
                    null,
                    offlineOrder.getLocalOrderId(),
                    detail.getProductId(),
                    detail.getQuantity(),
                    detail.getUnitPrice(),
                    detail.getLineTotal(),
                    detail.getNote(),
                    null,
                    null
            ));
        }
        return details;
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

    public record PaymentResult(Order order, boolean customerCreated) {
    }
}
