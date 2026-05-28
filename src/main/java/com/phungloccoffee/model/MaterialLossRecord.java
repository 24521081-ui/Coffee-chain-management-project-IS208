package com.phungloccoffee.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaterialLossRecord {
    private String lossId;
    private String warehouseId;
    private String employeeId;
    private String materialId;
    private String materialName;
    private String unit;
    private BigDecimal quantity;
    private String reason;
    private String note;
    private String status;
    private LocalDateTime createdAt;

    public String getLossId() {
        return lossId;
    }

    public void setLossId(String lossId) {
        this.lossId = lossId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPersistedReason() {
        if (note == null || note.isBlank()) {
            return reason;
        }
        return reason + " | Ghi chú: " + note.trim();
    }
}
