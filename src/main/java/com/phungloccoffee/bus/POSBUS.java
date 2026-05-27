package com.phungloccoffee.bus;

import com.phungloccoffee.dao.OrderDAO;
import com.phungloccoffee.dao.ProductDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.model.Product;
import com.phungloccoffee.model.offline.OfflineOrder;
import com.phungloccoffee.model.offline.OfflineOrderDetail;
import com.phungloccoffee.offline.InventoryCache;
import com.phungloccoffee.offline.NetworkMonitor;
import com.phungloccoffee.offline.OfflineStorage;
import com.phungloccoffee.offline.SyncService;
import com.phungloccoffee.util.SessionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class POSBUS extends PermissionBUS {
    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final OfflineStorage offlineStorage = OfflineStorage.getInstance();
    private final NetworkMonitor networkMonitor = NetworkMonitor.getInstance();
    private final SyncService syncService = new SyncService();

    public List<Product> loadMenuProducts() throws DatabaseException, PermissionException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return productDAO.findProductsForPOS();
    }

    public Order createOrder(List<OrderDetail> details)
            throws ValidationException, PermissionException, DatabaseException {
        return createPendingOrder(details, null);
    }

    public Order createOrder(List<OrderDetail> details, BigDecimal expectedTotal)
            throws ValidationException, PermissionException, DatabaseException {
        return createPendingOrder(details, expectedTotal);
    }

    public Order createPendingOrder(List<OrderDetail> details, BigDecimal expectedTotal)
            throws ValidationException, PermissionException, DatabaseException {
        if (!networkMonitor.checkNow()) {
            return createOfflinePendingOrder(details, expectedTotal);
        }
        try {
            return createOnlinePendingOrder(details, expectedTotal);
        } catch (DatabaseException e) {
            if (!networkMonitor.checkNow()) {
                return createOfflinePendingOrder(details, expectedTotal);
            }
            throw e;
        }
    }

    public Order createOnlinePendingOrder(List<OrderDetail> details, BigDecimal expectedTotal)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH");
        if (details == null || details.isEmpty()) {
            throw new ValidationException("Gio hang chua co san pham.");
        }
        if (isBlank(SessionManager.getCurrentBranchId())) {
            throw new ValidationException("Tai khoan chua gan chi nhanh, khong the tao hoa don.");
        }
        if (isBlank(SessionManager.getCurrentEmployeeId())) {
            throw new ValidationException("Tai khoan chua gan nhan vien, khong the tao hoa don.");
        }

        List<OrderDetail> checkedDetails = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetail detail : details) {
            if (detail == null || isBlank(detail.getSanPhamId())) {
                throw new ValidationException("Chi tiet hoa don thieu san pham.");
            }
            if (detail.getSoLuong() == null || detail.getSoLuong().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("So luong san pham khong hop le.");
            }

            Optional<Product> productResult = productDAO.findById(detail.getSanPhamId());
            if (productResult.isEmpty()) {
                throw new ValidationException("San pham khong ton tai: " + detail.getSanPhamId());
            }
            Product product = productResult.get();
            if (!"THANH_PHAM".equals(product.getLoaiSanPham()) || product.getTrangThai() != 1) {
                throw new ValidationException("San pham dang tam ngung hoac khong the ban: " + product.getTenSanPham());
            }

            BigDecimal quantity = detail.getSoLuong();
            BigDecimal productPrice = product.getGiaBan() == null ? BigDecimal.ZERO : product.getGiaBan();
            BigDecimal unitPrice = detail.getDonGia() == null || detail.getDonGia().compareTo(BigDecimal.ZERO) < 0
                    ? productPrice
                    : detail.getDonGia();
            BigDecimal lineTotal = detail.getThanhTien() == null ? unitPrice.multiply(quantity) : detail.getThanhTien();
            total = total.add(lineTotal);
            OrderDetail checkedDetail = new OrderDetail(
                    generateId("CT"),
                    null,
                    product.getSanPhamId(),
                    quantity,
                    unitPrice,
                    lineTotal,
                    detail.getGhiChu(),
                    null,
                    null
            );
            checkedDetail.setToppings(detail.getToppings());
            checkedDetails.add(checkedDetail);
        }

        if (expectedTotal != null && total.compareTo(expectedTotal) != 0) {
            throw new ValidationException("Tong tien UI khong khop du lieu san pham trong he thong.");
        }

        Order order = new Order(
                generateId("DH"),
                null,
                SessionManager.getCurrentBranchId(),
                SessionManager.getCurrentEmployeeId(),
                "DANG_TAO",
                total,
                BigDecimal.ZERO,
                total,
                "CHUA_THANH_TOAN",
                LocalDateTime.now(),
                LocalDateTime.now());
        orderDAO.createOrder(order, checkedDetails);
        return order;
    }

    public Order createOfflinePendingOrder(List<OrderDetail> details, BigDecimal expectedTotal)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH");
        validateSessionForSale();
        if (details == null || details.isEmpty()) {
            throw new ValidationException("Gio hang chua co san pham.");
        }

        List<OfflineOrderDetail> offlineDetails = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetail detail : details) {
            if (detail == null || isBlank(detail.getSanPhamId())) {
                throw new ValidationException("Chi tiet hoa don thieu san pham.");
            }
            if (detail.getSoLuong() == null || detail.getSoLuong().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("So luong san pham khong hop le.");
            }
            BigDecimal lineTotal = detail.getThanhTien() == null
                    ? nullToZero(detail.getDonGia()).multiply(detail.getSoLuong())
                    : detail.getThanhTien();
            total = total.add(lineTotal);
            offlineDetails.add(new OfflineOrderDetail(
                    detail.getSanPhamId(),
                    detail.getSoLuong(),
                    nullToZero(detail.getDonGia()),
                    lineTotal,
                    detail.getGhiChu(),
                    detail.getToppings()
            ));
        }

        if (expectedTotal != null && total.compareTo(expectedTotal) != 0) {
            throw new ValidationException("Tong tien UI khong khop du lieu san pham trong he thong.");
        }

        try {
            String localOrderId = offlineStorage.nextLocalOrderId(SessionManager.getCurrentBranchId());
            OfflineOrder offlineOrder = new OfflineOrder();
            offlineOrder.setLocalOrderId(localOrderId);
            offlineOrder.setBranchId(SessionManager.getCurrentBranchId());
            offlineOrder.setCashierId(SessionManager.getCurrentEmployeeId());
            offlineOrder.setCashierName(SessionManager.getCurrentEmployeeName());
            offlineOrder.setTotalAmount(total);
            offlineOrder.setOrderStatus("DANG_TAO");
            offlineOrder.setPaymentStatus("CHUA_THANH_TOAN");
            offlineOrder.setSyncStatus(OfflineOrder.SYNC_PENDING);
            offlineOrder.setCreatedAt(LocalDateTime.now());
            offlineOrder.setUpdatedAt(LocalDateTime.now());
            offlineOrder.setDetails(offlineDetails);
            offlineStorage.addOrder(offlineOrder);

            return new Order(
                    localOrderId,
                    null,
                    offlineOrder.getBranchId(),
                    offlineOrder.getCashierId(),
                    offlineOrder.getOrderStatus(),
                    total,
                    BigDecimal.ZERO,
                    total,
                    offlineOrder.getPaymentStatus(),
                    offlineOrder.getCreatedAt(),
                    offlineOrder.getUpdatedAt()
            );
        } catch (Exception e) {
            throw new DatabaseException("Khong the luu don offline: " + e.getMessage(), e);
        }
    }

    public void refreshOnlineCaches(String branchId) throws DatabaseException {
        try {
            syncService.refreshInventoryCache(branchId);
        } catch (Exception e) {
            throw new DatabaseException("Khong the cap nhat cache ton kho: " + e.getMessage(), e);
        }
    }

    public boolean isOnline() {
        return networkMonitor.isOnline();
    }

    private void validateSessionForSale() throws ValidationException {
        if (isBlank(SessionManager.getCurrentBranchId())) {
            throw new ValidationException("Tai khoan chua gan chi nhanh, khong the tao hoa don.");
        }
        if (isBlank(SessionManager.getCurrentEmployeeId())) {
            throw new ValidationException("Tai khoan chua gan nhan vien, khong the tao hoa don.");
        }
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
