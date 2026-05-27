package com.phungloccoffee.model;

import java.time.LocalDateTime;

public class KhachHang {
    private String khachHangId;
    private String hoTen;
    private String phone;
    private String email;
    private String hangThanhVien;
    private int diemTichLuy;
    private String ghiChu;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int trangThai = 1;

    public KhachHang() {
    }

    public KhachHang(String khachHangId, String hoTen, String phone, String email, int trangThai) {
        this.khachHangId = khachHangId;
        this.hoTen = hoTen;
        this.phone = phone;
        this.email = email;
        this.trangThai = trangThai;
    }

    public KhachHang(String khachHangId, String hoTen, String phone, String email,
                     String hangThanhVien, int diemTichLuy, String ghiChu,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.khachHangId = khachHangId;
        this.hoTen = hoTen;
        this.phone = phone;
        this.email = email;
        this.hangThanhVien = hangThanhVien;
        this.diemTichLuy = diemTichLuy;
        this.ghiChu = ghiChu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getKhachHangId() { return khachHangId; }
    public void setKhachHangId(String khachHangId) { this.khachHangId = khachHangId; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHangThanhVien() { return hangThanhVien; }
    public void setHangThanhVien(String hangThanhVien) { this.hangThanhVien = hangThanhVien; }
    public int getDiemTichLuy() { return diemTichLuy; }
    public void setDiemTichLuy(int diemTichLuy) { this.diemTichLuy = diemTichLuy; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getTrangThai() { return trangThai; }
    public void setTrangThai(int trangThai) { this.trangThai = trangThai; }
    @Override public String toString() { return hoTen; }
}
