package com.phungloccoffee.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WarehouseSlip {
    private String slipId;
    private String slipType;
    private String khoId;
    private String supplierId;
    private String createdBy;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String status;
    private String reason;
    private String note;
    private String rejectedReason;
    private List<WarehouseSlipLine> lines = new ArrayList<>();

    public String getSlipId() { return slipId; }
    public void setSlipId(String slipId) { this.slipId = slipId; }
    public String getSlipType() { return slipType; }
    public void setSlipType(String slipType) { this.slipType = slipType; }
    public String getKhoId() { return khoId; }
    public void setKhoId(String khoId) { this.khoId = khoId; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public List<WarehouseSlipLine> getLines() { return lines; }
    public void setLines(List<WarehouseSlipLine> lines) { this.lines = lines; }

    public BigDecimal getTotalQuantity() {
        return lines.stream()
                .map(line -> line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
