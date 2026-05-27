package com.phungloccoffee.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDetail {
    private String chiTietDonHangId;
    private String donHangId;
    private String sanPhamId;
    private BigDecimal soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private String ghiChu;
    private List<OrderDetailTopping> toppings = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderDetail() {
    }

    public OrderDetail(String chiTietDonHangId, String donHangId, String sanPhamId, BigDecimal soLuong,
                       BigDecimal donGia, BigDecimal thanhTien, String ghiChu, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.chiTietDonHangId = chiTietDonHangId;
        this.donHangId = donHangId;
        this.sanPhamId = sanPhamId;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.thanhTien = thanhTien;
        this.ghiChu = ghiChu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getChiTietDonHangId() { return chiTietDonHangId; }
    public void setChiTietDonHangId(String chiTietDonHangId) { this.chiTietDonHangId = chiTietDonHangId; }
    public String getDonHangId() { return donHangId; }
    public void setDonHangId(String donHangId) { this.donHangId = donHangId; }
    public String getSanPhamId() { return sanPhamId; }
    public void setSanPhamId(String sanPhamId) { this.sanPhamId = sanPhamId; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<OrderDetailTopping> getToppings() { return toppings; }
    public void setToppings(List<OrderDetailTopping> toppings) {
        this.toppings = toppings == null ? new ArrayList<>() : new ArrayList<>(toppings);
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getLineTotal() { return thanhTien; }
    public String getProductId() { return sanPhamId; }
    public int getQuantity() { return soLuong == null ? 0 : soLuong.intValue(); }
    public BigDecimal getUnitPrice() { return donGia; }

    @Override
    public String toString() {
        return sanPhamId + " x " + soLuong;
    }
}
