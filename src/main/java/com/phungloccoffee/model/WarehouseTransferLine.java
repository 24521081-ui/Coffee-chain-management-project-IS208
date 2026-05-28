package com.phungloccoffee.model;

import java.math.BigDecimal;

public class WarehouseTransferLine {
    private String itemId;
    private String itemName;
    private String unit;
    private BigDecimal quantity;

    public WarehouseTransferLine() {
    }

    public WarehouseTransferLine(String itemId, String itemName, String unit, BigDecimal quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unit = unit;
        this.quantity = quantity;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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
}
