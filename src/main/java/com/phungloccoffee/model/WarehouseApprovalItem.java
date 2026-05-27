package com.phungloccoffee.model;

import java.time.LocalDateTime;

public class WarehouseApprovalItem {
    private String slipId;
    private String slipType;
    private String khoId;
    private String createdBy;
    private String relatedParty;
    private String reason;
    private String status;
    private String rejectedReason;
    private int itemCount;
    private LocalDateTime createdAt;

    public String getSlipId() { return slipId; }
    public void setSlipId(String slipId) { this.slipId = slipId; }
    public String getSlipType() { return slipType; }
    public void setSlipType(String slipType) { this.slipType = slipType; }
    public String getKhoId() { return khoId; }
    public void setKhoId(String khoId) { this.khoId = khoId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getRelatedParty() { return relatedParty; }
    public void setRelatedParty(String relatedParty) { this.relatedParty = relatedParty; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
