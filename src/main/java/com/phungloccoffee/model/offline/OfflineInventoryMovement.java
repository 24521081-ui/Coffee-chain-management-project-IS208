package com.phungloccoffee.model.offline;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfflineInventoryMovement implements Serializable {
    private static final long serialVersionUID = 1L;

    private String localOrderId;
    private String productId;
    private BigDecimal quantityChange;
    private String movementType;
    private String syncStatus;
    private LocalDateTime createdAt;

    public OfflineInventoryMovement() {
    }

    public OfflineInventoryMovement(String localOrderId, String productId, BigDecimal quantityChange,
                                    String movementType, String syncStatus, LocalDateTime createdAt) {
        this.localOrderId = localOrderId;
        this.productId = productId;
        this.quantityChange = quantityChange;
        this.movementType = movementType;
        this.syncStatus = syncStatus;
        this.createdAt = createdAt;
    }

    public String getLocalOrderId() {
        return localOrderId;
    }

    public void setLocalOrderId(String localOrderId) {
        this.localOrderId = localOrderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
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
}
