package com.phungloccoffee.model.offline;

import com.phungloccoffee.model.OrderDetailTopping;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OfflineOrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String note;
    private List<OrderDetailTopping> toppings = new ArrayList<>();

    public OfflineOrderDetail() {
    }

    public OfflineOrderDetail(String productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal, String note) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.note = note;
    }

    public OfflineOrderDetail(String productId, BigDecimal quantity, BigDecimal unitPrice,
                              BigDecimal lineTotal, String note, List<OrderDetailTopping> toppings) {
        this(productId, quantity, unitPrice, lineTotal, note);
        setToppings(toppings);
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<OrderDetailTopping> getToppings() {
        return toppings;
    }

    public void setToppings(List<OrderDetailTopping> toppings) {
        this.toppings = toppings == null ? new ArrayList<>() : new ArrayList<>(toppings);
    }
}
