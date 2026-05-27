package com.phungloccoffee.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDetailTopping implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String orderDetailId;
    private String toppingProductId;
    private String toppingName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderDetailTopping() {
    }

    public OrderDetailTopping(String id, String orderDetailId, String toppingProductId, String toppingName,
                              BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderDetailId = orderDetailId;
        this.toppingProductId = toppingProductId;
        this.toppingName = toppingName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderDetailId() { return orderDetailId; }
    public void setOrderDetailId(String orderDetailId) { this.orderDetailId = orderDetailId; }
    public String getToppingProductId() { return toppingProductId; }
    public void setToppingProductId(String toppingProductId) { this.toppingProductId = toppingProductId; }
    public String getToppingName() { return toppingName; }
    public void setToppingName(String toppingName) { this.toppingName = toppingName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
