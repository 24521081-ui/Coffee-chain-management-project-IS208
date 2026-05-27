package com.phungloccoffee.model.offline;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OfflineOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SYNC_PENDING = "PENDING";
    public static final String SYNC_SYNCED = "SYNCED";
    public static final String SYNC_FAILED = "FAILED";

    private String localOrderId;
    private String serverOrderId;
    private String branchId;
    private String cashierId;
    private String cashierName;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastError;
    private List<OfflineOrderDetail> details = new ArrayList<>();
    private List<OfflineInventoryMovement> inventoryMovements = new ArrayList<>();

    public String getLocalOrderId() {
        return localOrderId;
    }

    public void setLocalOrderId(String localOrderId) {
        this.localOrderId = localOrderId;
    }

    public String getServerOrderId() {
        return serverOrderId;
    }

    public void setServerOrderId(String serverOrderId) {
        this.serverOrderId = serverOrderId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public List<OfflineOrderDetail> getDetails() {
        return details;
    }

    public void setDetails(List<OfflineOrderDetail> details) {
        this.details = details == null ? new ArrayList<>() : details;
    }

    public List<OfflineInventoryMovement> getInventoryMovements() {
        return inventoryMovements;
    }

    public void setInventoryMovements(List<OfflineInventoryMovement> inventoryMovements) {
        this.inventoryMovements = inventoryMovements == null ? new ArrayList<>() : inventoryMovements;
    }
}
