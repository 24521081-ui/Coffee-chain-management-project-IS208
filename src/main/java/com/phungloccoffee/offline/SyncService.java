package com.phungloccoffee.offline;

import com.phungloccoffee.dao.InventoryDAO;
import com.phungloccoffee.dao.OrderDAO;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.model.offline.OfflineOrder;
import com.phungloccoffee.model.offline.OfflineOrderDetail;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class SyncService {
    private final OfflineStorage offlineStorage = OfflineStorage.getInstance();
    private final InventoryCache inventoryCache = InventoryCache.getInstance();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public int syncPending(String branchId) throws Exception {
        List<OfflineOrder> pendingOrders = offlineStorage.findPendingOrders();
        int synced = 0;
        for (OfflineOrder offlineOrder : pendingOrders) {
            if (branchId != null && !branchId.isBlank() && !branchId.equals(offlineOrder.getBranchId())) {
                continue;
            }
            try {
                syncOneOrder(offlineOrder);
                synced++;
            } catch (Exception e) {
                offlineOrder.setLastError(e.getMessage());
                offlineOrder.setSyncStatus(isConnectionError(e) ? OfflineOrder.SYNC_PENDING : OfflineOrder.SYNC_FAILED);
                offlineOrder.setUpdatedAt(LocalDateTime.now());
                offlineStorage.updateOrder(offlineOrder);
            }
        }
        refreshInventoryCache(branchId);
        return synced;
    }

    public void refreshInventoryCache(String branchId) throws Exception {
        if (branchId == null || branchId.isBlank()) {
            return;
        }
        InventoryDAO.BranchInventorySnapshot snapshot = inventoryDAO.findByBranch(branchId);
        List<InventoryItem> items = snapshot.getItems();
        inventoryCache.loadFromList(branchId, snapshot.getKhoId(), items);
        inventoryCache.loadRecipes(inventoryDAO.findAllRecipeDeductions());
    }

    private void syncOneOrder(OfflineOrder offlineOrder) throws Exception {
        String serverOrderId = offlineOrder.getServerOrderId();
        if (serverOrderId == null || serverOrderId.isBlank()) {
            serverOrderId = generateServerOrderId(offlineOrder.getLocalOrderId());
            offlineOrder.setServerOrderId(serverOrderId);
        }

        Order order = new Order(
                serverOrderId,
                null,
                offlineOrder.getBranchId(),
                offlineOrder.getCashierId(),
                offlineOrder.getOrderStatus(),
                nullToZero(offlineOrder.getTotalAmount()),
                BigDecimal.ZERO,
                nullToZero(offlineOrder.getTotalAmount()),
                offlineOrder.getPaymentStatus(),
                offlineOrder.getCreatedAt(),
                LocalDateTime.now()
        );

        List<OrderDetail> details = new ArrayList<>();
        int index = 1;
        for (OfflineOrderDetail detail : offlineOrder.getDetails()) {
            OrderDetail orderDetail = new OrderDetail(
                    generateDetailId(offlineOrder.getLocalOrderId(), index++, detail.getProductId()),
                    serverOrderId,
                    detail.getProductId(),
                    nullToZero(detail.getQuantity()),
                    nullToZero(detail.getUnitPrice()),
                    nullToZero(detail.getLineTotal()),
                    detail.getNote(),
                    null,
                    null
            );
            orderDetail.setToppings(detail.getToppings());
            details.add(orderDetail);
        }

        boolean deductInventory = "DA_THANH_TOAN".equals(offlineOrder.getPaymentStatus());
        orderDAO.syncOfflineOrder(order, details, deductInventory);

        offlineOrder.setServerOrderId(serverOrderId);
        offlineOrder.setSyncStatus(OfflineOrder.SYNC_SYNCED);
        offlineOrder.setLastError(null);
        offlineOrder.setUpdatedAt(LocalDateTime.now());
        offlineStorage.updateOrder(offlineOrder);
    }

    private String generateServerOrderId(String localOrderId) {
        return "OF" + hash8(localOrderId);
    }

    private String generateDetailId(String localOrderId, int index, String productId) {
        return "OD" + hash8(localOrderId + "|" + index + "|" + productId);
    }

    private String hash8(String value) {
        CRC32 crc32 = new CRC32();
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        crc32.update(bytes, 0, bytes.length);
        return String.format("%08X", crc32.getValue());
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isConnectionError(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return message.contains("connection")
                || message.contains("ket noi")
                || message.contains("kết nối")
                || message.contains("io error")
                || message.contains("timeout");
    }
}
