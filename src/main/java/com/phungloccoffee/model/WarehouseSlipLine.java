package com.phungloccoffee.model;

import java.math.BigDecimal;

public class WarehouseSlipLine {
    private String itemId;
    private String itemName;
    private String unit;
    private BigDecimal systemQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String note;

    public WarehouseSlipLine() {
    }

    public WarehouseSlipLine(String itemId, String itemName, String unit, BigDecimal systemQuantity,
                             BigDecimal actualQuantity, BigDecimal quantity, BigDecimal unitPrice, String note) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unit = unit;
        this.systemQuantity = systemQuantity;
        this.actualQuantity = actualQuantity;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.note = note;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getSystemQuantity() { return systemQuantity; }
    public void setSystemQuantity(BigDecimal systemQuantity) { this.systemQuantity = systemQuantity; }
    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
