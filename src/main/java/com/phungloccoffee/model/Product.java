package com.phungloccoffee.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sanPhamId;
    private String danhMucId;
    private String tenDanhMuc;
    private String tenSanPham;
    private String loaiSanPham;
    private String donViTinh;
    private BigDecimal giaBan;
    private BigDecimal giaVon;
    private int trangThai;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(String sanPhamId, String danhMucId, String tenSanPham, String loaiSanPham, String donViTinh,
                   BigDecimal giaBan, BigDecimal giaVon, int trangThai, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(sanPhamId, danhMucId, null, tenSanPham, loaiSanPham, donViTinh, giaBan, giaVon, trangThai, createdAt, updatedAt);
    }

    public Product(String sanPhamId, String danhMucId, String tenDanhMuc, String tenSanPham, String loaiSanPham,
                   String donViTinh, BigDecimal giaBan, BigDecimal giaVon, int trangThai,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.sanPhamId = sanPhamId;
        this.danhMucId = danhMucId;
        this.tenDanhMuc = tenDanhMuc;
        this.tenSanPham = tenSanPham;
        this.loaiSanPham = loaiSanPham;
        this.donViTinh = donViTinh;
        this.giaBan = giaBan;
        this.giaVon = giaVon;
        this.trangThai = trangThai;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Product(int ignoredId, String code, String name, int ignoredCategoryId, BigDecimal price, String status) {
        this(code, null, name, "THANH_PHAM", "ML", price, BigDecimal.ZERO, "ACTIVE".equalsIgnoreCase(status) ? 1 : 0, null, null);
    }

    public String getSanPhamId() { return sanPhamId; }
    public void setSanPhamId(String sanPhamId) { this.sanPhamId = sanPhamId; }
    public String getDanhMucId() { return danhMucId; }
    public void setDanhMucId(String danhMucId) { this.danhMucId = danhMucId; }
    public String getTenDanhMuc() { return tenDanhMuc; }
    public void setTenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; }
    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }
    public String getLoaiSanPham() { return loaiSanPham; }
    public void setLoaiSanPham(String loaiSanPham) { this.loaiSanPham = loaiSanPham; }
    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }
    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
    public BigDecimal getGiaVon() { return giaVon; }
    public void setGiaVon(BigDecimal giaVon) { this.giaVon = giaVon; }
    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCode() { return sanPhamId; }
    public void setCode(String code) { this.sanPhamId = code; }
    public String getName() { return tenSanPham; }
    public void setName(String name) { this.tenSanPham = name; }
    public String getCategoryName() { return tenDanhMuc == null || tenDanhMuc.isBlank() ? danhMucId : tenDanhMuc; }
    public BigDecimal getPrice() { return giaBan; }
    public void setPrice(BigDecimal price) { this.giaBan = price; }
    public String getStatus() { return trangThai == 1 ? "Đang hoạt động" : "Ngừng bán"; }
    public void setStatus(String status) {
        this.trangThai = "1".equals(status)
                || "ACTIVE".equalsIgnoreCase(status)
                || "Đang hoạt động".equalsIgnoreCase(status) ? 1 : 0;
    }

    @Override
    public String toString() {
        return tenSanPham;
    }
}
