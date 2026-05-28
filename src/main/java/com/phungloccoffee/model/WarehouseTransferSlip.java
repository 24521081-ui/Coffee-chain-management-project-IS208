package com.phungloccoffee.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WarehouseTransferSlip {
    private String slipId;
    private String sourceWarehouseId;
    private String sourceWarehouseName;
    private String targetWarehouseId;
    private String targetWarehouseName;
    private String createdBy;
    private String status;
    private LocalDateTime createdAt;
    private List<WarehouseTransferLine> lines = new ArrayList<>();

    public String getSlipId() {
        return slipId;
    }

    public void setSlipId(String slipId) {
        this.slipId = slipId;
    }

    public String getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(String sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public String getSourceWarehouseName() {
        return sourceWarehouseName;
    }

    public void setSourceWarehouseName(String sourceWarehouseName) {
        this.sourceWarehouseName = sourceWarehouseName;
    }

    public String getTargetWarehouseId() {
        return targetWarehouseId;
    }

    public void setTargetWarehouseId(String targetWarehouseId) {
        this.targetWarehouseId = targetWarehouseId;
    }

    public String getTargetWarehouseName() {
        return targetWarehouseName;
    }

    public void setTargetWarehouseName(String targetWarehouseName) {
        this.targetWarehouseName = targetWarehouseName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public List<WarehouseTransferLine> getLines() {
        return lines;
    }

    public void setLines(List<WarehouseTransferLine> lines) {
        this.lines = lines;
    }
}
