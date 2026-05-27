package com.phungloccoffee.bus;

import com.phungloccoffee.dao.ChiTietDonHangDAO;
import com.phungloccoffee.dao.OrderDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.model.offline.OfflineInventoryMovement;
import com.phungloccoffee.model.offline.OfflineOrder;
import com.phungloccoffee.model.offline.OfflineOrderDetail;
import com.phungloccoffee.offline.InventoryCache;
import com.phungloccoffee.offline.NetworkMonitor;
import com.phungloccoffee.offline.OfflineStorage;
import com.phungloccoffee.offline.SyncService;
import com.phungloccoffee.util.ValidationUtils;

import java.io.IOException;
import java.math.BigDecimal;
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
            "The ngan hang"
    );

    private final OrderDAO orderDAO = new OrderDAO();
    private final ChiTietDonHangDAO detailDAO = new ChiTietDonHangDAO();
    private final OfflineStorage offlineStorage = OfflineStorage.getInstance();
    private final InventoryCache inventoryCache = InventoryCache.getInstance();
    private final NetworkMonitor networkMonitor = NetworkMonitor.getInstance();
    private final SyncService syncService = new SyncService();

    public void pay(int orderId, String method, BigDecimal amount)
            throws ValidationException, PermissionException, DatabaseException {
        pay(String.valueOf(orderId), method, amount);
    }

    public void pay(String orderId, String method, BigDecimal amount)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH");
        ValidationUtils.requireText(orderId, "Ma hoa don");
        ValidationUtils.requireText(method, "Phuong thuc thanh toan");
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
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH");
        ValidationUtils.requireText(orderId, "Ma hoa don");
        ValidationUtils.requireText(paymentMethod, "Phuong thuc thanh toan");
        if (!ALLOWED_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new ValidationException("Phương thức thanh toán không hợp lệ.");
        }

        Optional<OfflineOrder> offlineOrder = findOfflineOrder(orderId);
        if (offlineOrder.isPresent()) {
            return confirmOfflinePayment(offlineOrder.get(), paymentMethod);
        }
        if (!networkMonitor.checkNow()) {
            throw new ValidationException("Đơn này chưa có trong lưu trữ offline nên không thể xác nhận khi mất kết nối Oracle.");
        }

        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy đơn hàng: " + orderId));
        if (PAID_STATUS.equals(order.getTrangThaiThanhToan())) {
            throw new ValidationException("Đơn hàng này đã được thanh toán");
        }

        Order paidOrder = orderDAO.confirmPayment(orderId);
        try {
            syncService.refreshInventoryCache(paidOrder.getChiNhanhId());
        } catch (Exception ignored) {
        }
        return paidOrder;
    }

    public Order getOrderForPayment(String orderId)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(orderId, "Ma hoa don");
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
        ValidationUtils.requireText(orderId, "Ma hoa don");
        Optional<OfflineOrder> offlineOrder = findOfflineOrder(orderId);
        if (offlineOrder.isPresent()) {
            return toOrderDetails(offlineOrder.get());
        }
        return detailDAO.findByDonHangId(orderId);
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
}
