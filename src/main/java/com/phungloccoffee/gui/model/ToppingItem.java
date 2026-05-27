package com.phungloccoffee.gui.model;

import java.math.BigDecimal;

public class ToppingItem {
    private String toppingId;
    private String toppingName;
    private BigDecimal price;
    private int quantity;

    public ToppingItem() {
    }

    public ToppingItem(String toppingId, String toppingName, BigDecimal price) {
        this(toppingId, toppingName, price, 1);
    }

    public ToppingItem(String toppingId, String toppingName, BigDecimal price, int quantity) {
        this.toppingId = toppingId;
        this.toppingName = toppingName;
        this.price = price == null ? BigDecimal.ZERO : price;
        this.quantity = Math.max(0, quantity);
    }

    public String getToppingId() {
        return toppingId;
    }

    public void setToppingId(String toppingId) {
        this.toppingId = toppingId;
    }

    public String getToppingName() {
        return toppingName;
    }

    public void setToppingName(String toppingName) {
        this.toppingName = toppingName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price == null ? BigDecimal.ZERO : price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public BigDecimal calculateSubtotal(int mainProductQuantity) {
        return price.multiply(BigDecimal.valueOf(quantity)).multiply(BigDecimal.valueOf(Math.max(1, mainProductQuantity)));
    }
}
