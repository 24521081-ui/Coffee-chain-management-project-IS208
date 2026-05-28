SET DEFINE OFF;
SET SERVEROUTPUT ON SIZE UNLIMITED;
SET FEEDBACK ON;
SET VERIFY OFF;

PROMPT =========================================================
PROMPT DROP TABLES - PHUNG LOC COFFEE MANAGEMENT
PROMPT =========================================================

DECLARE
    PROCEDURE drop_table_if_exists(p_table_name VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP TABLE ' || p_table_name || ' CASCADE CONSTRAINTS PURGE';
        DBMS_OUTPUT.PUT_LINE('Dropped table: ' || p_table_name);
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -942 THEN
                DBMS_OUTPUT.PUT_LINE('Skip missing table: ' || p_table_name);
            ELSE
                RAISE;
            END IF;
    END;
BEGIN
    drop_table_if_exists('password_reset_otp');
    drop_table_if_exists('chi_tiet_dinh_muc');
    drop_table_if_exists('dinh_muc_san_pham');
    drop_table_if_exists('hao_hut_nguyen_lieu');

    drop_table_if_exists('chi_tiet_kiem_ke_kho');
    drop_table_if_exists('kiem_ke_kho');

    drop_table_if_exists('chi_tiet_phieu_dieu_chuyen_kho');
    drop_table_if_exists('phieu_dieu_chuyen_kho');

    drop_table_if_exists('chi_tiet_xuat_kho');
    drop_table_if_exists('phieu_xuat_kho');

    drop_table_if_exists('chi_tiet_nhap_kho');
    drop_table_if_exists('phieu_nhap_kho');

    drop_table_if_exists('giao_dich_offline');

    drop_table_if_exists('chi_tiet_don_hang');
    drop_table_if_exists('don_hang');

    drop_table_if_exists('ton_kho');
    drop_table_if_exists('san_pham');
    drop_table_if_exists('danh_muc_san_pham');

    drop_table_if_exists('nha_cung_cap');
    drop_table_if_exists('kho');
    drop_table_if_exists('pos_device');
    drop_table_if_exists('khach_hang');
    drop_table_if_exists('nhan_vien');
    drop_table_if_exists('app_user');
    drop_table_if_exists('chi_nhanh');
END;
/

PROMPT =========================================================
PROMPT CREATE TABLES - PHUNG LOC COFFEE MANAGEMENT
PROMPT =========================================================

/* =========================================================
   1. CHI_NHANH
   ========================================================= */
CREATE TABLE chi_nhanh (
    chi_nhanh_id      VARCHAR2(10) NOT NULL,
    ten_chi_nhanh     NVARCHAR2(120) NOT NULL,
    phone             VARCHAR2(20),
    email             VARCHAR2(254),
    dia_chi           NVARCHAR2(200),
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_chi_nhanh PRIMARY KEY (chi_nhanh_id),
    CONSTRAINT uq_chi_nhanh_email UNIQUE (email),
    CONSTRAINT ck_chi_nhanh_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   2. APP_USER
   ========================================================= */
CREATE TABLE app_user (
    user_id           VARCHAR2(10) NOT NULL,
    ten_dang_nhap     NVARCHAR2(120) NOT NULL,
    mat_khau          VARCHAR2(255) NOT NULL,
    vai_tro           NVARCHAR2(50) NOT NULL,
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    last_login        TIMESTAMP(6) WITH TIME ZONE,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_app_user PRIMARY KEY (user_id),
    CONSTRAINT uq_app_user_username UNIQUE (ten_dang_nhap),
    CONSTRAINT ck_app_user_vai_tro CHECK (
        vai_tro IN (
            'THU_NGAN',
            'QUAN_LY_CHI_NHANH',
            'NHAN_VIEN_KHO',
            'IT_ADMIN',
            'BAN_GIAM_DOC'
        )
    ),
    CONSTRAINT ck_app_user_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   3. NHAN_VIEN
   ========================================================= */
CREATE TABLE nhan_vien (
    nhan_vien_id      VARCHAR2(10) NOT NULL,
    user_id           VARCHAR2(10),
    chi_nhanh_id      VARCHAR2(10),
    ho_ten            NVARCHAR2(120) NOT NULL,
    cccd              VARCHAR2(50),
    email             VARCHAR2(254),
    phone             VARCHAR2(20),
    chuc_vu           NVARCHAR2(50) NOT NULL,
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    ngay_vao_lam      TIMESTAMP(6) WITH TIME ZONE,
    ghi_chu           CLOB,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_nhan_vien PRIMARY KEY (nhan_vien_id),
    CONSTRAINT fk_nv_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT fk_nv_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh(chi_nhanh_id),
    CONSTRAINT uq_nv_user UNIQUE (user_id),
    CONSTRAINT uq_nv_cccd UNIQUE (cccd),
    CONSTRAINT uq_nv_email UNIQUE (email),
    CONSTRAINT uq_nv_phone UNIQUE (phone),
    CONSTRAINT ck_nv_chuc_vu CHECK (
        chuc_vu IN (
            'THU_NGAN',
            'QUAN_LY_CHI_NHANH',
            'NHAN_VIEN_KHO',
            'IT_ADMIN',
            'BAN_GIAM_DOC',
            'NHAN_VIEN_PHA_CHE',
            'NHAN_VIEN_PHUC_VU'
        )
    ),
    CONSTRAINT ck_nv_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   4. KHACH_HANG
   ========================================================= */
CREATE TABLE khach_hang (
    khach_hang_id     VARCHAR2(10) NOT NULL,
    ho_ten            NVARCHAR2(120) NOT NULL,
    phone             VARCHAR2(20),
    email             VARCHAR2(254),
    hang_thanh_vien   NVARCHAR2(30) DEFAULT 'THUONG' NOT NULL,
    diem_tich_luy     NUMBER(10) DEFAULT 0 NOT NULL,
    ghi_chu           CLOB,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_khach_hang PRIMARY KEY (khach_hang_id),
    CONSTRAINT uq_kh_phone UNIQUE (phone),
    CONSTRAINT uq_kh_email UNIQUE (email),
    CONSTRAINT ck_kh_diem CHECK (diem_tich_luy >= 0),
    CONSTRAINT ck_kh_hang CHECK (hang_thanh_vien IN ('THUONG', 'BAC', 'VANG', 'KIM_CUONG'))
);

/* =========================================================
   5. POS_DEVICE
   ========================================================= */
CREATE TABLE pos_device (
    pos_id            VARCHAR2(10) NOT NULL,
    chi_nhanh_id      VARCHAR2(10) NOT NULL,
    ma_may            VARCHAR2(50) NOT NULL,
    ten_may           NVARCHAR2(100),
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    ghi_chu           CLOB,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_pos_device PRIMARY KEY (pos_id),
    CONSTRAINT fk_pos_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh(chi_nhanh_id),
    CONSTRAINT uq_pos_ma_may UNIQUE (ma_may),
    CONSTRAINT ck_pos_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   6. KHO
   ========================================================= */
CREATE TABLE kho (
    kho_id            VARCHAR2(10) NOT NULL,
    chi_nhanh_id      VARCHAR2(10) NOT NULL,
    ten_kho           NVARCHAR2(120) NOT NULL,
    dia_chi           NVARCHAR2(200),
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_kho PRIMARY KEY (kho_id),
    CONSTRAINT fk_kho_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh(chi_nhanh_id),
    CONSTRAINT ck_kho_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   7. NHA_CUNG_CAP
   ========================================================= */
CREATE TABLE nha_cung_cap (
    nha_cung_cap_id   VARCHAR2(10) NOT NULL,
    ten_nha_cung_cap  NVARCHAR2(100) NOT NULL,
    so_dien_thoai     VARCHAR2(20),
    email             VARCHAR2(254),
    dia_chi           NVARCHAR2(200),
    trang_thai        NVARCHAR2(30) DEFAULT 'CON_HOP_TAC' NOT NULL,
    ghi_chu           CLOB,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_nha_cung_cap PRIMARY KEY (nha_cung_cap_id),
    CONSTRAINT uq_ncc_email UNIQUE (email),
    CONSTRAINT uq_ncc_phone UNIQUE (so_dien_thoai),
    CONSTRAINT ck_ncc_trang_thai CHECK (trang_thai IN ('CON_HOP_TAC', 'NGUNG_HOP_TAC'))
);

/* =========================================================
   8. DANH_MUC_SAN_PHAM
   ========================================================= */
CREATE TABLE danh_muc_san_pham (
    danh_muc_id       VARCHAR2(10) NOT NULL,
    ten_danh_muc      NVARCHAR2(100) NOT NULL,
    mo_ta             CLOB,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_danh_muc_sp PRIMARY KEY (danh_muc_id),
    CONSTRAINT uq_danh_muc_sp_name UNIQUE (ten_danh_muc)
);

/* =========================================================
   9. SAN_PHAM
   ========================================================= */
CREATE TABLE san_pham (
    san_pham_id       VARCHAR2(10) NOT NULL,
    danh_muc_id       VARCHAR2(10) NOT NULL,
    ten_san_pham      NVARCHAR2(120) NOT NULL,
    loai_san_pham     NVARCHAR2(30) NOT NULL,
    don_vi_tinh       VARCHAR2(10) NOT NULL,
    gia_ban           NUMBER(12,2) DEFAULT 0 NOT NULL,
    gia_von           NUMBER(12,2) DEFAULT 0 NOT NULL,
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_san_pham PRIMARY KEY (san_pham_id),
    CONSTRAINT fk_sp_danh_muc FOREIGN KEY (danh_muc_id) REFERENCES danh_muc_san_pham(danh_muc_id),
    CONSTRAINT ck_sp_gia_ban CHECK (gia_ban >= 0),
    CONSTRAINT ck_sp_gia_von CHECK (gia_von >= 0),
    CONSTRAINT ck_sp_loai CHECK (loai_san_pham IN ('THANH_PHAM', 'NGUYEN_LIEU', 'BAN_THANH_PHAM')),
    CONSTRAINT ck_sp_don_vi CHECK (UPPER(don_vi_tinh) IN ('ML', 'L', 'MG', 'KG')),
    CONSTRAINT ck_sp_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   10. TON_KHO
   ========================================================= */
CREATE TABLE ton_kho (
    kho_id             VARCHAR2(10) NOT NULL,
    san_pham_id        VARCHAR2(10) NOT NULL,
    so_luong_ton       NUMBER(12,2) DEFAULT 0 NOT NULL,
    muc_ton_toi_thieu  NUMBER(12,2) DEFAULT 0,
    last_updated       TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ton_kho PRIMARY KEY (kho_id, san_pham_id),
    CONSTRAINT fk_tk_kho FOREIGN KEY (kho_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_tk_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_tk_so_luong CHECK (so_luong_ton >= 0),
    CONSTRAINT ck_tk_muc_ton CHECK (muc_ton_toi_thieu IS NULL OR muc_ton_toi_thieu >= 0)
);

/* =========================================================
   11. DON_HANG
   ========================================================= */
CREATE TABLE don_hang (
    don_hang_id              VARCHAR2(10) NOT NULL,
    khach_hang_id            VARCHAR2(10),
    chi_nhanh_id             VARCHAR2(10) NOT NULL,
    nhan_vien_id             VARCHAR2(10) NOT NULL,
    trang_thai               NVARCHAR2(30) DEFAULT 'DANG_TAO' NOT NULL,
    tam_tinh                 NUMBER(12,2) DEFAULT 0 NOT NULL,
    giam_gia                 NUMBER(12,2) DEFAULT 0 NOT NULL,
    tong_tien                NUMBER(12,2) DEFAULT 0 NOT NULL,
    trang_thai_thanh_toan    NVARCHAR2(30) DEFAULT 'CHUA_THANH_TOAN' NOT NULL,
    created_at               TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at               TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_don_hang PRIMARY KEY (don_hang_id),
    CONSTRAINT fk_dh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
    CONSTRAINT fk_dh_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh(chi_nhanh_id),
    CONSTRAINT fk_dh_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT ck_dh_tam_tinh CHECK (tam_tinh >= 0),
    CONSTRAINT ck_dh_giam_gia CHECK (giam_gia >= 0),
    CONSTRAINT ck_dh_tong_tien CHECK (tong_tien >= 0),
    CONSTRAINT ck_dh_trang_thai CHECK (trang_thai IN ('DANG_TAO', 'DA_HOAN_THANH', 'DA_HUY')),
    CONSTRAINT ck_dh_thanh_toan CHECK (trang_thai_thanh_toan IN ('CHUA_THANH_TOAN', 'DA_THANH_TOAN', 'DA_HOAN_TIEN'))
);

/* =========================================================
   12. CHI_TIET_DON_HANG
   ========================================================= */
CREATE TABLE chi_tiet_don_hang (
    chi_tiet_don_hang_id VARCHAR2(10) NOT NULL,
    don_hang_id          VARCHAR2(10) NOT NULL,
    san_pham_id          VARCHAR2(10) NOT NULL,
    so_luong             NUMBER(12,2) NOT NULL,
    don_gia              NUMBER(12,2) NOT NULL,
    thanh_tien           NUMBER(12,2) NOT NULL,
    ghi_chu              CLOB,
    created_at           TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at           TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ct_don_hang PRIMARY KEY (chi_tiet_don_hang_id),
    CONSTRAINT fk_ctdh_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id),
    CONSTRAINT fk_ctdh_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_ctdh_so_luong CHECK (so_luong > 0),
    CONSTRAINT ck_ctdh_don_gia CHECK (don_gia >= 0),
    CONSTRAINT ck_ctdh_thanh_tien CHECK (thanh_tien >= 0)
);

/* =========================================================
   13. GIAO_DICH_OFFLINE
   ========================================================= */
CREATE TABLE giao_dich_offline (
    giao_dich_id        VARCHAR2(10) NOT NULL,
    pos_id              VARCHAR2(10) NOT NULL,
    don_hang_id         VARCHAR2(10),
    thoi_gian_tao       TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    thoi_gian_dong_bo   TIMESTAMP(6) WITH TIME ZONE,
    trang_thai_dong_bo  NVARCHAR2(30) DEFAULT 'CHO_DONG_BO' NOT NULL,
    trang_thai          NUMBER(1) DEFAULT 1 NOT NULL,
    created_at          TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at          TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_giao_dich_offline PRIMARY KEY (giao_dich_id),
    CONSTRAINT fk_gdo_pos FOREIGN KEY (pos_id) REFERENCES pos_device(pos_id),
    CONSTRAINT fk_gdo_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id),
    CONSTRAINT ck_gdo_sync CHECK (trang_thai_dong_bo IN ('CHO_DONG_BO', 'DA_DONG_BO', 'LOI_DONG_BO')),
    CONSTRAINT ck_gdo_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   14. PHIEU_NHAP_KHO
   ========================================================= */
CREATE TABLE phieu_nhap_kho (
    phieu_nhap_id      VARCHAR2(10) NOT NULL,
    kho_id             VARCHAR2(10) NOT NULL,
    nha_cung_cap_id    VARCHAR2(10),
    nhan_vien_id       VARCHAR2(10) NOT NULL,
    ngay_nhap          TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    tong_so_luong      NUMBER(12,2) DEFAULT 0 NOT NULL,
    so_luong_mat_hang  NUMBER(12,2) DEFAULT 0 NOT NULL,
    trang_thai         NVARCHAR2(30) DEFAULT 'NHAP' NOT NULL,
    ghi_chu            CLOB,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_phieu_nhap PRIMARY KEY (phieu_nhap_id),
    CONSTRAINT fk_pn_kho FOREIGN KEY (kho_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_pn_ncc FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(nha_cung_cap_id),
    CONSTRAINT fk_pn_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT ck_pn_tong_so_luong CHECK (tong_so_luong >= 0),
    CONSTRAINT ck_pn_mat_hang CHECK (so_luong_mat_hang >= 0),
    CONSTRAINT ck_pn_trang_thai CHECK (trang_thai IN ('NHAP', 'DA_DUYET', 'DA_HUY'))
);

/* =========================================================
   15. CHI_TIET_NHAP_KHO
   ========================================================= */
CREATE TABLE chi_tiet_nhap_kho (
    phieu_nhap_id      VARCHAR2(10) NOT NULL,
    san_pham_id        VARCHAR2(10) NOT NULL,
    so_luong           NUMBER(12,2) NOT NULL,
    don_vi_tinh        VARCHAR2(10) NOT NULL,
    thanh_tien         NUMBER(12,2) DEFAULT 0 NOT NULL,
    ghi_chu            CLOB,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ct_nhap_kho PRIMARY KEY (phieu_nhap_id, san_pham_id),
    CONSTRAINT fk_ctnk_phieu FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho(phieu_nhap_id),
    CONSTRAINT fk_ctnk_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_ctnk_so_luong CHECK (so_luong > 0),
    CONSTRAINT ck_ctnk_thanh_tien CHECK (thanh_tien >= 0),
    CONSTRAINT ck_ctnk_don_vi CHECK (UPPER(don_vi_tinh) IN ('ML', 'L', 'MG', 'KG'))
);

/* =========================================================
   16. PHIEU_XUAT_KHO
   ========================================================= */
CREATE TABLE phieu_xuat_kho (
    phieu_xuat_id      VARCHAR2(10) NOT NULL,
    kho_id             VARCHAR2(10) NOT NULL,
    nhan_vien_id       VARCHAR2(10) NOT NULL,
    ngay_xuat          TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    ly_do_xuat         NVARCHAR2(100),
    so_luong_mat_hang  NUMBER(12,2) DEFAULT 0 NOT NULL,
    tong_tien          NUMBER(12,2) DEFAULT 0 NOT NULL,
    trang_thai         NVARCHAR2(30) DEFAULT 'NHAP' NOT NULL,
    ghi_chu            CLOB,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_phieu_xuat PRIMARY KEY (phieu_xuat_id),
    CONSTRAINT fk_px_kho FOREIGN KEY (kho_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_px_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT ck_px_mat_hang CHECK (so_luong_mat_hang >= 0),
    CONSTRAINT ck_px_tong_tien CHECK (tong_tien >= 0),
    CONSTRAINT ck_px_trang_thai CHECK (trang_thai IN ('NHAP', 'DA_DUYET', 'DA_HUY'))
);

/* =========================================================
   17. CHI_TIET_XUAT_KHO
   ========================================================= */
CREATE TABLE chi_tiet_xuat_kho (
    phieu_xuat_id      VARCHAR2(10) NOT NULL,
    san_pham_id        VARCHAR2(10) NOT NULL,
    so_luong           NUMBER(12,2) NOT NULL,
    don_vi_tinh        VARCHAR2(10) NOT NULL,
    ghi_chu            CLOB,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ct_xuat_kho PRIMARY KEY (phieu_xuat_id, san_pham_id),
    CONSTRAINT fk_ctxk_phieu FOREIGN KEY (phieu_xuat_id) REFERENCES phieu_xuat_kho(phieu_xuat_id),
    CONSTRAINT fk_ctxk_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_ctxk_so_luong CHECK (so_luong > 0),
    CONSTRAINT ck_ctxk_don_vi CHECK (UPPER(don_vi_tinh) IN ('ML', 'L', 'MG', 'KG'))
);

/* =========================================================
   18. PHIEU_DIEU_CHUYEN_KHO
   ========================================================= */
CREATE TABLE phieu_dieu_chuyen_kho (
    phieu_dieu_chuyen_id VARCHAR2(10) NOT NULL,
    kho_nguon_id         VARCHAR2(10) NOT NULL,
    kho_dich_id          VARCHAR2(10) NOT NULL,
    nhan_vien_id         VARCHAR2(10) NOT NULL,
    ngay_lap             TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    trang_thai           NVARCHAR2(30) DEFAULT 'NHAP' NOT NULL,
    ghi_chu              CLOB,
    created_at           TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at           TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_pdc_kho PRIMARY KEY (phieu_dieu_chuyen_id),
    CONSTRAINT fk_pdc_kho_nguon FOREIGN KEY (kho_nguon_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_pdc_kho_dich FOREIGN KEY (kho_dich_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_pdc_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT ck_pdc_kho_khac CHECK (kho_nguon_id <> kho_dich_id),
    CONSTRAINT ck_pdc_trang_thai CHECK (trang_thai IN ('NHAP', 'DA_DUYET', 'DA_HUY'))
);

/* =========================================================
   19. CHI_TIET_PHIEU_DIEU_CHUYEN_KHO
   Da sua loi du dau "\" sau updated_at.
   ========================================================= */
CREATE TABLE chi_tiet_phieu_dieu_chuyen_kho (
    phieu_dieu_chuyen_id VARCHAR2(10) NOT NULL,
    san_pham_id          VARCHAR2(10) NOT NULL,
    so_luong_dieu_chuyen NUMBER(12,2) NOT NULL,
    don_vi_tinh          VARCHAR2(10) NOT NULL,
    ghi_chu              CLOB,
    created_at           TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at           TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ct_pdc PRIMARY KEY (phieu_dieu_chuyen_id, san_pham_id),
    CONSTRAINT fk_ctpdc_phieu FOREIGN KEY (phieu_dieu_chuyen_id) REFERENCES phieu_dieu_chuyen_kho(phieu_dieu_chuyen_id),
    CONSTRAINT fk_ctpdc_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_ctpdc_so_luong CHECK (so_luong_dieu_chuyen > 0),
    CONSTRAINT ck_ctpdc_don_vi CHECK (UPPER(don_vi_tinh) IN ('ML', 'L', 'MG', 'KG'))
);

/* =========================================================
   20. KIEM_KE_KHO
   ========================================================= */
CREATE TABLE kiem_ke_kho (
    kiem_ke_id        VARCHAR2(10) NOT NULL,
    kho_id            VARCHAR2(10) NOT NULL,
    nhan_vien_id      VARCHAR2(10) NOT NULL,
    ngay_kiem_ke      TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    trang_thai        NVARCHAR2(30) DEFAULT 'NHAP' NOT NULL,
    ghi_chu           CLOB,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_kiem_ke PRIMARY KEY (kiem_ke_id),
    CONSTRAINT fk_kk_kho FOREIGN KEY (kho_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_kk_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT ck_kk_trang_thai CHECK (trang_thai IN ('NHAP', 'DA_HOAN_THANH', 'DA_HUY'))
);

/* =========================================================
   21. CHI_TIET_KIEM_KE_KHO
   ========================================================= */
CREATE TABLE chi_tiet_kiem_ke_kho (
    kiem_ke_id         VARCHAR2(10) NOT NULL,
    san_pham_id        VARCHAR2(10) NOT NULL,
    so_luong_he_thong  NUMBER(12,2) DEFAULT 0 NOT NULL,
    so_luong_thuc_te   NUMBER(12,2) DEFAULT 0 NOT NULL,
    do_lech            NUMBER(12,2),
    chenh_lech_ty_le   NUMBER(5,2),
    ghi_chu            CLOB,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ct_kiem_ke PRIMARY KEY (kiem_ke_id, san_pham_id),
    CONSTRAINT fk_ctkk_kiem_ke FOREIGN KEY (kiem_ke_id) REFERENCES kiem_ke_kho(kiem_ke_id),
    CONSTRAINT fk_ctkk_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_ctkk_sl_ht CHECK (so_luong_he_thong >= 0),
    CONSTRAINT ck_ctkk_sl_tt CHECK (so_luong_thuc_te >= 0)
);

/* =========================================================
   22. DINH_MUC_SAN_PHAM
   ========================================================= */
CREATE TABLE dinh_muc_san_pham (
    dinh_muc_id       VARCHAR2(10) NOT NULL,
    san_pham_ban_id   VARCHAR2(10) NOT NULL,
    ten_dinh_muc      NVARCHAR2(100),
    mo_ta             CLOB,
    trang_thai        NUMBER(1) DEFAULT 1 NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at        TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_dinh_muc_sp PRIMARY KEY (dinh_muc_id),
    CONSTRAINT fk_dmsp_san_pham_ban FOREIGN KEY (san_pham_ban_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_dmsp_trang_thai CHECK (trang_thai IN (0, 1))
);

/* =========================================================
   23. CHI_TIET_DINH_MUC
   ========================================================= */
CREATE TABLE chi_tiet_dinh_muc (
    dinh_muc_id           VARCHAR2(10) NOT NULL,
    san_pham_nguyen_lieu  VARCHAR2(10) NOT NULL,
    so_luong_dung         NUMBER(12,2) NOT NULL,
    don_vi_tinh           VARCHAR2(10) NOT NULL,
    ghi_chu               CLOB,
    created_at            TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at            TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_ct_dinh_muc PRIMARY KEY (dinh_muc_id, san_pham_nguyen_lieu),
    CONSTRAINT fk_ctdm_dinh_muc FOREIGN KEY (dinh_muc_id) REFERENCES dinh_muc_san_pham(dinh_muc_id),
    CONSTRAINT fk_ctdm_nguyen_lieu FOREIGN KEY (san_pham_nguyen_lieu) REFERENCES san_pham(san_pham_id),
    CONSTRAINT ck_ctdm_so_luong CHECK (so_luong_dung > 0),
    CONSTRAINT ck_ctdm_don_vi CHECK (UPPER(don_vi_tinh) IN ('ML', 'L', 'MG', 'KG'))
);

/* =========================================================
   24. HAO_HUT_NGUYEN_LIEU
   ========================================================= */
CREATE TABLE hao_hut_nguyen_lieu (
    hao_hut_id         VARCHAR2(10) NOT NULL,
    kho_id             VARCHAR2(10) NOT NULL,
    san_pham_id        VARCHAR2(10) NOT NULL,
    nhan_vien_id       VARCHAR2(10) NOT NULL,
    so_luong_hao_hut   NUMBER(12,2) NOT NULL,
    don_vi_tinh        VARCHAR2(10) NOT NULL,
    ly_do_hao_hut      NVARCHAR2(100),
    ghi_chu            CLOB,
    thoi_gian_ghi_nhan TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    trang_thai         NVARCHAR2(30) DEFAULT 'CHO_DUYET' NOT NULL,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,

    CONSTRAINT pk_hao_hut PRIMARY KEY (hao_hut_id),
    CONSTRAINT fk_hh_kho FOREIGN KEY (kho_id) REFERENCES kho(kho_id),
    CONSTRAINT fk_hh_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id),
    CONSTRAINT fk_hh_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id),
    CONSTRAINT ck_hh_so_luong CHECK (so_luong_hao_hut > 0),
    CONSTRAINT ck_hh_don_vi CHECK (UPPER(don_vi_tinh) IN ('ML', 'L', 'MG', 'KG')),
    CONSTRAINT ck_hh_trang_thai CHECK (trang_thai IN ('CHO_DUYET', 'DA_DUYET', 'TU_CHOI'))
);

/* =========================================================
   25. PASSWORD_RESET_OTP
   ========================================================= */
CREATE TABLE password_reset_otp (
    otp_id             VARCHAR2(10) NOT NULL,
    user_id            VARCHAR2(10) NOT NULL,
    otp_code_hash      VARCHAR2(255) NOT NULL,
    phuong_thuc        NVARCHAR2(20) NOT NULL,
    dia_chi_nhan       VARCHAR2(255),
    reset_token        VARCHAR2(255),
    thoi_gian_het_han  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    da_xac_thuc        NUMBER(1) DEFAULT 0 NOT NULL,
    da_su_dung         NUMBER(1) DEFAULT 0 NOT NULL,
    so_lan_sai         NUMBER DEFAULT 0 NOT NULL,
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP,
    verified_at        TIMESTAMP(6) WITH TIME ZONE,
    used_at            TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT pk_password_reset_otp PRIMARY KEY (otp_id),
    CONSTRAINT fk_pro_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT ck_pro_phuong_thuc CHECK (phuong_thuc IN ('EMAIL', 'SMS')),
    -- 0: chua xac thuc OTP
    -- 1: da xac thuc OTP
    CONSTRAINT ck_pro_da_xac_thuc CHECK (da_xac_thuc IN (0, 1)),
    -- 0: chua su dung de doi mat khau
    -- 1: da su dung de doi mat khau
    CONSTRAINT ck_pro_da_su_dung CHECK (da_su_dung IN (0, 1)),
    CONSTRAINT ck_pro_so_lan_sai CHECK (so_lan_sai >= 0)
);

PROMPT =========================================================
PROMPT DONE: DROP + CREATE TABLES COMPLETED
PROMPT =========================================================

SET DEFINE OFF;

SET SERVEROUTPUT ON SIZE UNLIMITED;

SET FEEDBACK ON;

SET VERIFY OFF;

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

ALTER SESSION SET NLS_DATE_FORMAT = 'DD/MM/YYYY';

ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'DD/MM/YYYY HH24:MI:SS';

ALTER SESSION SET NLS_TIMESTAMP_TZ_FORMAT = 'DD/MM/YYYY HH24:MI:SS TZH:TZM';

PROMPT ==================================================
PROMPT TAO DU LIEU MAU - PHUNG LOC COFFEE
PROMPT Schema target: dung dung ten bang va thuoc tinh da cung cap
PROMPT Khoang du lieu demo: 27/05/2026 den 02/06/2026
PROMPT Mat khau demo chung: 123456
PROMPT ==================================================

PROMPT ==================================================
PROMPT 1. LUU Y SCHEMA
PROMPT ==================================================

-- File nay KHONG ALTER TABLE, KHONG them cot, KHONG doi ten bang/cot.
-- Tat ca INSERT/DELETE duoc viet theo dung schema:
--   pos_device.pos_id, ma_may, ten_may
--   dinh_muc_san_pham.san_pham_ban_id
--   chi_tiet_dinh_muc.san_pham_nguyen_lieu, so_luong_dung
--   don_hang khong co phuong_thuc_thanh_toan / paid_at
--   chi_tiet_xuat_kho khong co chi_tiet_xuat_id / don_gia
-- Luu y constraint san_pham:
--   loai_san_pham chi nhan THANH_PHAM / NGUYEN_LIEU / BAN_THANH_PHAM
--   don_vi_tinh chi nhan ML / L / MG / KG
-- Vi vay topping duoc seed voi loai_san_pham = BAN_THANH_PHAM.

BEGIN
    DBMS_OUTPUT.PUT_LINE('Seed file dang chay theo dung schema hien tai, khong thay doi cau truc bang.');
END;
/

PROMPT ==================================================
PROMPT 2. RESET DU LIEU DEMO CU
PROMPT ==================================================

DECLARE
    TYPE id_list IS TABLE OF VARCHAR2(10);
    v_demo_user_ids id_list := id_list();
    v_count NUMBER;
BEGIN
    SELECT DISTINCT user_id
    BULK COLLECT INTO v_demo_user_ids
    FROM nhan_vien
    WHERE user_id IS NOT NULL
      AND (
          REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$')
          OR nhan_vien_id IN ('NVCEO', 'NVIT')
          OR nhan_vien_id LIKE 'NVQL%'
          OR nhan_vien_id LIKE 'NVT%'
          OR nhan_vien_id LIKE 'NVK%'
          OR nhan_vien_id LIKE 'NVPC%'
          OR nhan_vien_id LIKE 'NVPV%'
      );

    DELETE FROM chi_tiet_xuat_kho
    WHERE phieu_xuat_id IN (
        SELECT phieu_xuat_id
        FROM phieu_xuat_kho
        WHERE phieu_xuat_id LIKE 'PXD%'
           OR REGEXP_LIKE(kho_id, '^KHO00[1-8]$')
    )
       OR REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$');

    DELETE FROM phieu_xuat_kho
    WHERE phieu_xuat_id LIKE 'PXD%'
       OR REGEXP_LIKE(kho_id, '^KHO00[1-8]$');

    DELETE FROM chi_tiet_nhap_kho
    WHERE phieu_nhap_id IN (
        SELECT phieu_nhap_id
        FROM phieu_nhap_kho
        WHERE phieu_nhap_id LIKE 'PND%'
           OR REGEXP_LIKE(kho_id, '^KHO00[1-8]$')
    )
       OR REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$');

    DELETE FROM phieu_nhap_kho
    WHERE phieu_nhap_id LIKE 'PND%'
       OR REGEXP_LIKE(kho_id, '^KHO00[1-8]$')
       OR nha_cung_cap_id LIKE 'NCCDEMO%';

    DELETE FROM chi_tiet_phieu_dieu_chuyen_kho
    WHERE phieu_dieu_chuyen_id IN (
        SELECT phieu_dieu_chuyen_id
        FROM phieu_dieu_chuyen_kho
        WHERE phieu_dieu_chuyen_id LIKE 'DCD%'
           OR REGEXP_LIKE(kho_nguon_id, '^KHO00[1-8]$')
           OR REGEXP_LIKE(kho_dich_id, '^KHO00[1-8]$')
    )
       OR REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$');

    DELETE FROM phieu_dieu_chuyen_kho
    WHERE phieu_dieu_chuyen_id LIKE 'DCD%'
       OR REGEXP_LIKE(kho_nguon_id, '^KHO00[1-8]$')
       OR REGEXP_LIKE(kho_dich_id, '^KHO00[1-8]$');

    DELETE FROM chi_tiet_kiem_ke_kho
    WHERE kiem_ke_id IN (
        SELECT kiem_ke_id
        FROM kiem_ke_kho
        WHERE kiem_ke_id LIKE 'KKD%'
           OR REGEXP_LIKE(kho_id, '^KHO00[1-8]$')
    )
       OR REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$');

    DELETE FROM kiem_ke_kho
    WHERE kiem_ke_id LIKE 'KKD%'
       OR REGEXP_LIKE(kho_id, '^KHO00[1-8]$');

    DELETE FROM giao_dich_offline
    WHERE REGEXP_LIKE(pos_id, '^POS00[1-8]$')
       OR pos_id IN (
            SELECT pos_id
            FROM pos_device
            WHERE REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$')
       )
       OR don_hang_id IN (
            SELECT don_hang_id
            FROM don_hang
            WHERE don_hang_id LIKE 'DHD%'
       );

    DELETE FROM chi_tiet_don_hang
    WHERE don_hang_id IN (
        SELECT don_hang_id
        FROM don_hang
        WHERE don_hang_id LIKE 'DHD%'
           OR REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$')
    )
       OR REGEXP_LIKE(san_pham_id, '^SP0(0[1-9]|[1-2][0-9]|30)$');

    DELETE FROM don_hang
    WHERE don_hang_id LIKE 'DHD%'
       OR REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$')
       OR khach_hang_id LIKE 'KHDEMO%';

    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'PASSWORD_RESET_OTP';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE q'[
            DELETE FROM password_reset_otp
            WHERE user_id IN (
                SELECT user_id
                FROM app_user
                WHERE user_id IN ('UCEO', 'UIT')
                   OR user_id LIKE 'UQL%'
                   OR user_id LIKE 'UTN%'
                   OR user_id LIKE 'UKHO%'
                   OR LOWER(ten_dang_nhap) IN ('ceo', 'itadmin')
                   OR LOWER(ten_dang_nhap) LIKE 'ql.cn%'
                   OR LOWER(ten_dang_nhap) LIKE 'thungan.cn%'
                   OR LOWER(ten_dang_nhap) LIKE 'kho.cn%'
            )
        ]';

        IF v_demo_user_ids.COUNT > 0 THEN
            FORALL i IN 1..v_demo_user_ids.COUNT
                EXECUTE IMMEDIATE 'DELETE FROM password_reset_otp WHERE user_id = :1' USING v_demo_user_ids(i);
        END IF;
    END IF;

    DELETE FROM ton_kho
    WHERE REGEXP_LIKE(kho_id, '^KHO00[1-8]$')
       OR REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$');

    DELETE FROM chi_tiet_dinh_muc
    WHERE dinh_muc_id IN (
        SELECT dinh_muc_id
        FROM dinh_muc_san_pham
        WHERE dinh_muc_id LIKE 'DMD%'
           OR REGEXP_LIKE(san_pham_ban_id, '^SP0(0[1-9]|[1-2][0-9]|30)$')
    )
       OR REGEXP_LIKE(san_pham_nguyen_lieu, '^NL0(0[1-9]|1[0-9]|2[0-8])$');

    DELETE FROM dinh_muc_san_pham
    WHERE dinh_muc_id LIKE 'DMD%'
       OR REGEXP_LIKE(san_pham_ban_id, '^SP0(0[1-9]|[1-2][0-9]|30)$');

    DELETE FROM hao_hut_nguyen_lieu
    WHERE REGEXP_LIKE(kho_id, '^KHO00[1-8]$')
       OR REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$')
       OR nhan_vien_id IN (
            SELECT nhan_vien_id
            FROM nhan_vien
            WHERE REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$')
       );

    DELETE FROM pos_device
    WHERE REGEXP_LIKE(pos_id, '^POS00[1-8]$')
       OR REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$');

    DELETE FROM nhan_vien
    WHERE REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$')
       OR nhan_vien_id IN ('NVCEO', 'NVIT')
       OR nhan_vien_id LIKE 'NVQL%'
       OR nhan_vien_id LIKE 'NVT%'
       OR nhan_vien_id LIKE 'NVK%'
       OR nhan_vien_id LIKE 'NVPC%'
       OR nhan_vien_id LIKE 'NVPV%';

    IF v_demo_user_ids.COUNT > 0 THEN
        FORALL i IN 1..v_demo_user_ids.COUNT
            DELETE FROM app_user WHERE user_id = v_demo_user_ids(i);
    END IF;

    DELETE FROM app_user
    WHERE user_id IN ('UCEO', 'UIT')
       OR user_id LIKE 'UQL%'
       OR user_id LIKE 'UTN%'
       OR user_id LIKE 'UKHO%'
       OR LOWER(ten_dang_nhap) IN ('ceo', 'itadmin')
       OR LOWER(ten_dang_nhap) LIKE 'ql.cn%'
       OR LOWER(ten_dang_nhap) LIKE 'thungan.cn%'
       OR LOWER(ten_dang_nhap) LIKE 'kho.cn%';

    DELETE FROM khach_hang WHERE khach_hang_id LIKE 'KHDEMO%';
    DELETE FROM kho WHERE REGEXP_LIKE(kho_id, '^KHO00[1-8]$');
    DELETE FROM chi_nhanh WHERE REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$');
    DELETE FROM san_pham WHERE REGEXP_LIKE(san_pham_id, '^(SP0(0[1-9]|[1-2][0-9]|30)|NL0(0[1-9]|1[0-9]|2[0-8]))$');
    DELETE FROM danh_muc_san_pham WHERE danh_muc_id LIKE 'DMC%';
    DELETE FROM nha_cung_cap WHERE nha_cung_cap_id LIKE 'NCCDEMO%';

    COMMIT;
END;
/

PROMPT ==================================================
PROMPT 3. INSERT CHI NHANH
PROMPT ==================================================

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN001', N'Trung tâm', '02873010001', 'cn001@phungloc.demo', N'120 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-01-10 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN002', N'Phú Nhuận', '02873010002', 'cn002@phungloc.demo', N'45 Phan Xích Long, Phường 2, Quận Phú Nhuận, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-02-18 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN003', N'Bình Thạnh', '02873010003', 'cn003@phungloc.demo', N'210 Điện Biên Phủ, Phường 15, Quận Bình Thạnh, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-03-05 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN004', N'Gò Vấp', '02873010004', 'cn004@phungloc.demo', N'18 Quang Trung, Phường 10, Quận Gò Vấp, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-03-28 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN005', N'Thủ Đức', '02873010005', 'cn005@phungloc.demo', N'35 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-04-12 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN006', N'Tân Bình', '02873010006', 'cn006@phungloc.demo', N'92 Cộng Hòa, Phường 4, Quận Tân Bình, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-05-01 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN007', N'Quận 7', '02873010007', 'cn007@phungloc.demo', N'68 Nguyễn Thị Thập, Phường Tân Phong, Quận 7, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-05-20 08:00:00', '+07:00'), SYSTIMESTAMP);

INSERT INTO chi_nhanh (chi_nhanh_id, ten_chi_nhanh, phone, email, dia_chi, trang_thai, created_at, updated_at)
VALUES ('CN008', N'Quận 10', '02873010008', 'cn008@phungloc.demo', N'285 Cách Mạng Tháng 8, Phường 12, Quận 10, TP.HCM', 1, FROM_TZ(TIMESTAMP '2025-06-08 08:00:00', '+07:00'), SYSTIMESTAMP);

COMMIT;

PROMPT ==================================================
PROMPT 4. INSERT KHO VA POS_DEVICE
PROMPT ==================================================

BEGIN
    FOR b IN 1..8 LOOP
        INSERT INTO kho (kho_id, chi_nhanh_id, ten_kho, dia_chi, trang_thai, created_at, updated_at)
        VALUES (
            'KHO' || LPAD(b, 3, '0'),
            'CN' || LPAD(b, 3, '0'),
            N'Kho chính CN' || LPAD(b, 3, '0'),
            N'Cùng địa chỉ chi nhánh CN' || LPAD(b, 3, '0'),
            1,
            SYSTIMESTAMP,
            SYSTIMESTAMP
        );

        INSERT INTO pos_device (pos_id, chi_nhanh_id, ma_may, ten_may, trang_thai, ghi_chu, created_at, updated_at)
        VALUES (
            'POS' || LPAD(b, 3, '0'),
            'CN' || LPAD(b, 3, '0'),
            'POS-CN' || LPAD(b, 3, '0'),
            N'Máy POS demo CN' || LPAD(b, 3, '0'),
            1,
            N'Thiết bị POS demo',
            SYSTIMESTAMP,
            SYSTIMESTAMP
        );
    END LOOP;
END;
/

COMMIT;

PROMPT ==================================================
PROMPT 5. INSERT NHA CUNG CAP
PROMPT ==================================================

INSERT INTO nha_cung_cap (nha_cung_cap_id, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, trang_thai, ghi_chu, created_at, updated_at)
VALUES ('NCCDEMO001', N'Công ty Cà phê Cao Nguyên', '02870020001', 'caphe@phungloc.demo', N'Đắk Lắk', 'CON_HOP_TAC', N'Cung cấp cà phê', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO nha_cung_cap (nha_cung_cap_id, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, trang_thai, ghi_chu, created_at, updated_at)
VALUES ('NCCDEMO002', N'Sữa và Kem An Lành', '02870020002', 'sua@phungloc.demo', N'TP.HCM', 'CON_HOP_TAC', N'Cung cấp sữa và kem', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO nha_cung_cap (nha_cung_cap_id, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, trang_thai, ghi_chu, created_at, updated_at)
VALUES ('NCCDEMO003', N'Nguyên liệu Trà Việt', '02870020003', 'tra@phungloc.demo', N'Lâm Đồng', 'CON_HOP_TAC', N'Cung cấp trà và syrup', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO nha_cung_cap (nha_cung_cap_id, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, trang_thai, ghi_chu, created_at, updated_at)
VALUES ('NCCDEMO004', N'Xưởng Bánh Bơ Thơm', '02870020004', 'banh@phungloc.demo', N'TP.HCM', 'CON_HOP_TAC', N'Cung cấp nguyên liệu bánh', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO nha_cung_cap (nha_cung_cap_id, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, trang_thai, ghi_chu, created_at, updated_at)
VALUES ('NCCDEMO005', N'Bao bì Xanh Sài Gòn', '02870020005', 'baobi@phungloc.demo', N'TP.HCM', 'CON_HOP_TAC', N'Cung cấp vật tư tiêu hao', SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;

PROMPT ==================================================
PROMPT 6. INSERT APP_USER VA NHAN_VIEN
PROMPT ==================================================

DECLARE
    TYPE name_table IS TABLE OF NVARCHAR2(60) INDEX BY PLS_INTEGER;
    v_ho name_table;
    v_dem name_table;
    v_ten name_table;
    v_counter NUMBER := 0;

    FUNCTION branch_code(p_branch NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN LPAD(p_branch, 3, '0');
    END;

    FUNCTION ts_tz(p_date DATE) RETURN TIMESTAMP WITH TIME ZONE IS
    BEGIN
        RETURN FROM_TZ(CAST(p_date AS TIMESTAMP), '+07:00');
    END;

    FUNCTION demo_name(p_seed NUMBER) RETURN NVARCHAR2 IS
    BEGIN
        RETURN v_ho(MOD(p_seed - 1, 10) + 1)
            || N' '
            || v_dem(MOD(TRUNC((p_seed - 1) / 10), 10) + 1)
            || N' '
            || v_ten(MOD(TRUNC((p_seed - 1) / 100), 10) + 1);
    END;

    FUNCTION cccd_for(p_seed NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN '079' || LPAD(p_seed, 9, '0');
    END;

    FUNCTION phone_for(p_seed NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN '09' || LPAD(p_seed, 8, '0');
    END;

    PROCEDURE add_user(
        p_user_id VARCHAR2,
        p_username VARCHAR2,
        p_role VARCHAR2
    ) IS
    BEGIN
        INSERT INTO app_user (
            user_id,
            ten_dang_nhap,
            mat_khau,
            vai_tro,
            trang_thai,
            last_login,
            created_at,
            updated_at
        )
        VALUES (
            p_user_id,
            p_username,
            '123456',
            p_role,
            1,
            NULL,
            SYSTIMESTAMP,
            SYSTIMESTAMP
        );
    END;

    PROCEDURE add_employee (
    p_employee_id VARCHAR2,
    p_user_id VARCHAR2,
    p_branch_id VARCHAR2,
    p_name NVARCHAR2,
    p_position VARCHAR2,
    p_note NVARCHAR2,
    p_start DATE
) IS
    v_cccd  VARCHAR2(50);
    v_phone VARCHAR2(20);
BEGIN
    v_counter := v_counter + 1;

    v_cccd := cccd_for(v_counter);
    v_phone := phone_for(v_counter);

    INSERT INTO nhan_vien (
        nhan_vien_id,
        user_id,
        chi_nhanh_id,
        ho_ten,
        cccd,
        email,
        phone,
        chuc_vu,
        trang_thai,
        ngay_vao_lam,
        ghi_chu
    )
    VALUES (
        p_employee_id,
        p_user_id,
        p_branch_id,
        p_name,
        v_cccd,
        LOWER(p_employee_id) || '@phungloccoffee.demo',
        v_phone,
        p_position,
        1,
        p_start,
        p_note
    );
END;

    PROCEDURE add_user_employee(
        p_user_id VARCHAR2,
        p_username VARCHAR2,
        p_role VARCHAR2,
        p_employee_id VARCHAR2,
        p_branch_id VARCHAR2,
        p_name NVARCHAR2,
        p_position VARCHAR2,
        p_note NVARCHAR2,
        p_start DATE
    ) IS
    BEGIN
        add_user(p_user_id, p_username, p_role);
        add_employee(p_employee_id, p_user_id, p_branch_id, p_name, p_position, p_note, p_start);
    END;
BEGIN
    v_ho(1) := N'Nguyễn'; v_ho(2) := N'Trần'; v_ho(3) := N'Lê'; v_ho(4) := N'Phạm'; v_ho(5) := N'Hoàng';
    v_ho(6) := N'Phan'; v_ho(7) := N'Võ'; v_ho(8) := N'Đặng'; v_ho(9) := N'Bùi'; v_ho(10) := N'Đỗ';

    v_dem(1) := N'Minh'; v_dem(2) := N'Thanh'; v_dem(3) := N'Gia'; v_dem(4) := N'Hoài'; v_dem(5) := N'Quang';
    v_dem(6) := N'Ngọc'; v_dem(7) := N'Phương'; v_dem(8) := N'Khánh'; v_dem(9) := N'Tuấn'; v_dem(10) := N'Bảo';

    v_ten(1) := N'An'; v_ten(2) := N'Bình'; v_ten(3) := N'Chi'; v_ten(4) := N'Duy'; v_ten(5) := N'Hà';
    v_ten(6) := N'Khang'; v_ten(7) := N'Linh'; v_ten(8) := N'Nam'; v_ten(9) := N'Trúc'; v_ten(10) := N'Vy';

    add_user_employee('UCEO', 'ceo', 'BAN_GIAM_DOC', 'NVCEO', 'CN001', N'Nguyễn Phụng Lộc', 'BAN_GIAM_DOC', N'CEO toàn hệ thống', DATE '2025-01-01');
    add_user_employee('UIT', 'itadmin', 'IT_ADMIN', 'NVIT', 'CN001', N'Trần Quản Trị', 'IT_ADMIN', N'IT Admin toàn hệ thống', DATE '2025-01-01');

    FOR b IN 1..8 LOOP
        add_user_employee(
            'UQL' || branch_code(b),
            'ql.cn' || branch_code(b),
            'QUAN_LY_CHI_NHANH',
            'NVQL' || branch_code(b),
            'CN' || branch_code(b),
            demo_name(20 + b),
            'QUAN_LY_CHI_NHANH',
            N'Quản lý chi nhánh CN' || branch_code(b),
            DATE '2025-01-10' + b
        );

        FOR i IN 1..CASE WHEN b <= 4 THEN 5 ELSE 4 END LOOP
            add_user_employee(
                'UTN' || branch_code(b) || LPAD(i, 2, '0'),
                'thungan.cn' || branch_code(b) || '.' || LPAD(i, 2, '0'),
                'THU_NGAN',
                'NVT' || branch_code(b) || LPAD(i, 2, '0'),
                'CN' || branch_code(b),
                demo_name(100 + b * 10 + i),
                'THU_NGAN',
                N'Thu ngân ca bán hàng CN' || branch_code(b),
                DATE '2025-02-01' + b
            );
        END LOOP;

        FOR i IN 1..2 LOOP
            add_user_employee(
                'UKHO' || branch_code(b) || LPAD(i, 2, '0'),
                'kho.cn' || branch_code(b) || '.' || LPAD(i, 2, '0'),
                'NHAN_VIEN_KHO',
                'NVK' || branch_code(b) || LPAD(i, 2, '0'),
                'CN' || branch_code(b),
                demo_name(220 + b * 10 + i),
                'NHAN_VIEN_KHO',
                N'Nhân viên kho CN' || branch_code(b),
                DATE '2025-02-15' + b
            );
        END LOOP;

        FOR i IN 1..6 LOOP
            add_employee(
                'NVPC' || branch_code(b) || LPAD(i, 2, '0'),
                NULL,
                'CN' || branch_code(b),
                demo_name(360 + b * 10 + i),
                'NHAN_VIEN_PHA_CHE',
                N'Nhân viên pha chế CN' || branch_code(b),
                DATE '2025-03-01' + b
            );
        END LOOP;

        FOR i IN 1..5 LOOP
            add_employee(
                'NVPV' || branch_code(b) || LPAD(i, 2, '0'),
                NULL,
                'CN' || branch_code(b),
                demo_name(520 + b * 10 + i),
                'NHAN_VIEN_PHUC_VU',
                N'Nhân viên phục vụ CN' || branch_code(b),
                DATE '2025-03-15' + b
            );
        END LOOP;
    END LOOP;
END;
/

COMMIT;

PROMPT ==================================================
PROMPT 7. INSERT DANH_MUC_SAN_PHAM
PROMPT ==================================================

INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC001', N'Topping', N'Topping bán kèm đồ uống', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC002', N'Cà phê', N'Nhóm món cà phê bán tại POS', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC003', N'Trà', N'Nhóm món trà và trà sữa', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC004', N'Bánh', N'Nhóm bánh bán kèm', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC005', N'Nguyên liệu cà phê', N'Hạt và bột cà phê', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC006', N'Sữa và kem', N'Sữa, kem và phụ liệu béo', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC007', N'Trà và syrup', N'Trà, syrup và chất tạo ngọt', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC008', N'Topping nguyên liệu', N'Nguyên liệu làm topping', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC009', N'Nguyên liệu bánh', N'Nguyên liệu sản xuất bánh', SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO danh_muc_san_pham (danh_muc_id, ten_danh_muc, mo_ta, created_at, updated_at) VALUES ('DMC010', N'Vật tư tiêu hao', N'Ly, nắp, ống hút và túi giấy', SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;

PROMPT ==================================================
PROMPT 8. INSERT SAN_PHAM/MENU
PROMPT ==================================================

-- Topping luu bang loai_san_pham = BAN_THANH_PHAM de khop constraint ck_sp_loai.
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP001', 'DMC001', N'Trân châu đen', 'BAN_THANH_PHAM', 'KG', 8000, 2500, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP002', 'DMC001', N'Thạch cà phê', 'BAN_THANH_PHAM', 'KG', 8000, 2500, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP003', 'DMC001', N'Kem cheese', 'BAN_THANH_PHAM', 'KG', 12000, 4500, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP004', 'DMC001', N'Pudding trứng', 'BAN_THANH_PHAM', 'KG', 10000, 3500, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP005', 'DMC001', N'Hạt chia', 'BAN_THANH_PHAM', 'KG', 7000, 2200, 1, SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP006', 'DMC002', N'Cà phê khò đen', 'THANH_PHAM', 'ML', 32000, 12000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP007', 'DMC002', N'Cà phê sữa đá', 'THANH_PHAM', 'ML', 35000, 13000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP008', 'DMC002', N'Bạc xỉu', 'THANH_PHAM', 'ML', 39000, 15000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP009', 'DMC002', N'Latte đá', 'THANH_PHAM', 'ML', 45000, 18000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP010', 'DMC002', N'Americano đá', 'THANH_PHAM', 'ML', 36000, 13000, 1, SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP011', 'DMC003', N'Trà đào cam sả', 'THANH_PHAM', 'ML', 45000, 16000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP012', 'DMC003', N'Trà vải hoa hồng', 'THANH_PHAM', 'ML', 45000, 16000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP013', 'DMC003', N'Trà sen vàng kem cheese', 'THANH_PHAM', 'ML', 49000, 19000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP014', 'DMC003', N'Trà mật ong chanh', 'THANH_PHAM', 'ML', 39000, 14000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP015', 'DMC003', N'Trà đen macchiato', 'THANH_PHAM', 'ML', 42000, 15000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP016', 'DMC003', N'Trà sữa truyền thống', 'THANH_PHAM', 'ML', 42000, 15000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP017', 'DMC003', N'Trà sữa trân châu', 'THANH_PHAM', 'ML', 48000, 18000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP018', 'DMC003', N'Trà ô long lạnh', 'THANH_PHAM', 'ML', 38000, 13000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP019', 'DMC003', N'Matcha latte', 'THANH_PHAM', 'ML', 49000, 20000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP020', 'DMC003', N'Trà đen kem cheese', 'THANH_PHAM', 'ML', 46000, 18000, 1, SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP021', 'DMC004', N'Croissant bơ', 'THANH_PHAM', 'KG', 39000, 16000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP022', 'DMC004', N'Bánh muffin việt quất', 'THANH_PHAM', 'KG', 35000, 14000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP023', 'DMC004', N'Tiramisu ly', 'THANH_PHAM', 'KG', 52000, 22000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP024', 'DMC004', N'Cheesecake chanh dây', 'THANH_PHAM', 'KG', 55000, 23000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP025', 'DMC004', N'Brownie chocolate', 'THANH_PHAM', 'KG', 42000, 17000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP026', 'DMC004', N'Bánh chuối nướng', 'THANH_PHAM', 'KG', 32000, 12000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP027', 'DMC004', N'Bánh tart trứng', 'THANH_PHAM', 'KG', 30000, 12000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP028', 'DMC004', N'Bánh flan', 'THANH_PHAM', 'KG', 25000, 9000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP029', 'DMC004', N'Bánh su kem', 'THANH_PHAM', 'KG', 30000, 11000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('SP030', 'DMC004', N'Donut đường', 'THANH_PHAM', 'KG', 28000, 10000, 1, SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;

PROMPT ==================================================
PROMPT 9. INSERT NGUYEN_LIEU TRONG BANG SAN_PHAM
PROMPT ==================================================

INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL001', 'DMC005', N'Hạt cà phê rang', 'NGUYEN_LIEU', 'KG', 0, 180000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL002', 'DMC005', N'Bột cà phê khò đen', 'NGUYEN_LIEU', 'KG', 0, 220000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL003', 'DMC006', N'Sữa đặc', 'NGUYEN_LIEU', 'KG', 0, 28000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL004', 'DMC006', N'Sữa tươi', 'NGUYEN_LIEU', 'L', 0, 32000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL005', 'DMC007', N'Đường nước', 'NGUYEN_LIEU', 'L', 0, 26000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL006', 'DMC007', N'Syrup đào', 'NGUYEN_LIEU', 'L', 0, 85000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL007', 'DMC007', N'Syrup vải', 'NGUYEN_LIEU', 'L', 0, 88000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL008', 'DMC007', N'Trà đen', 'NGUYEN_LIEU', 'KG', 0, 160000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL009', 'DMC007', N'Trà xanh', 'NGUYEN_LIEU', 'KG', 0, 170000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL010', 'DMC007', N'Bột matcha', 'NGUYEN_LIEU', 'KG', 0, 420000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL011', 'DMC006', N'Kem cheese', 'NGUYEN_LIEU', 'KG', 0, 135000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL012', 'DMC008', N'Trân châu', 'NGUYEN_LIEU', 'KG', 0, 65000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL013', 'DMC008', N'Thạch cà phê', 'NGUYEN_LIEU', 'KG', 0, 60000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL014', 'DMC008', N'Pudding', 'NGUYEN_LIEU', 'KG', 0, 75000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL015', 'DMC008', N'Hạt chia', 'NGUYEN_LIEU', 'KG', 0, 145000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL016', 'DMC007', N'Syrup sen vàng', 'NGUYEN_LIEU', 'L', 0, 90000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL017', 'DMC007', N'Mật ong', 'NGUYEN_LIEU', 'L', 0, 120000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL018', 'DMC007', N'Sả cây', 'NGUYEN_LIEU', 'KG', 0, 45000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL019', 'DMC009', N'Bột cacao', 'NGUYEN_LIEU', 'KG', 0, 190000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL020', 'DMC009', N'Bột bánh', 'NGUYEN_LIEU', 'KG', 0, 42000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL021', 'DMC009', N'Bơ', 'NGUYEN_LIEU', 'KG', 0, 155000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL022', 'DMC009', N'Trứng', 'NGUYEN_LIEU', 'KG', 0, 3500, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL023', 'DMC006', N'Kem tươi', 'NGUYEN_LIEU', 'L', 0, 95000, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL024', 'DMC010', N'Ly giấy', 'NGUYEN_LIEU', 'KG', 0, 950, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL025', 'DMC010', N'Nắp ly', 'NGUYEN_LIEU', 'KG', 0, 450, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL026', 'DMC010', N'Ống hút', 'NGUYEN_LIEU', 'KG', 0, 250, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL027', 'DMC010', N'Túi giấy bánh', 'NGUYEN_LIEU', 'KG', 0, 700, 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO san_pham (san_pham_id, danh_muc_id, ten_san_pham, loai_san_pham, don_vi_tinh, gia_ban, gia_von, trang_thai, created_at, updated_at) VALUES ('NL028', 'DMC007', N'Trà ô long', 'NGUYEN_LIEU', 'KG', 0, 185000, 1, SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;

PROMPT ==================================================
PROMPT 10. INSERT DINH_MUC_SAN_PHAM VA CHI_TIET_DINH_MUC
PROMPT ==================================================

DECLARE
    v_recipe_seq NUMBER := 0;
    v_current_recipe VARCHAR2(10);
    v_material_unit VARCHAR2(10);

    PROCEDURE begin_recipe(p_product_id VARCHAR2) IS
    BEGIN
        v_recipe_seq := v_recipe_seq + 1;
        v_current_recipe := 'DMD' || LPAD(v_recipe_seq, 3, '0');

        INSERT INTO dinh_muc_san_pham (
            dinh_muc_id,
            san_pham_ban_id,
            ten_dinh_muc,
            mo_ta,
            trang_thai,
            created_at,
            updated_at
        )
        VALUES (
            v_current_recipe,
            p_product_id,
            N'Định mức ' || p_product_id,
            N'Định mức demo cho sản phẩm bán ra',
            1,
            SYSTIMESTAMP,
            SYSTIMESTAMP
        );
    END;

    PROCEDURE recipe_line(p_material_id VARCHAR2, p_quantity NUMBER) IS
    BEGIN
        SELECT don_vi_tinh
        INTO v_material_unit
        FROM san_pham
        WHERE san_pham_id = p_material_id;

        INSERT INTO chi_tiet_dinh_muc (
            dinh_muc_id,
            san_pham_nguyen_lieu,
            so_luong_dung,
            don_vi_tinh,
            ghi_chu,
            created_at,
            updated_at
        )
        VALUES (
            v_current_recipe,
            p_material_id,
            p_quantity,
            v_material_unit,
            N'Dòng định mức demo',
            SYSTIMESTAMP,
            SYSTIMESTAMP
        );
    END;
BEGIN
    begin_recipe('SP001'); recipe_line('NL012', 0.05); recipe_line('NL005', 0.01);
    begin_recipe('SP002'); recipe_line('NL013', 0.05);
    begin_recipe('SP003'); recipe_line('NL011', 0.04);
    begin_recipe('SP004'); recipe_line('NL014', 0.05); recipe_line('NL022', 0.05);
    begin_recipe('SP005'); recipe_line('NL015', 0.02); recipe_line('NL005', 0.01);

    begin_recipe('SP006'); recipe_line('NL002', 0.02); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP007'); recipe_line('NL001', 0.03); recipe_line('NL003', 0.08); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP008'); recipe_line('NL001', 0.02); recipe_line('NL003', 0.10); recipe_line('NL004', 0.12); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP009'); recipe_line('NL001', 0.02); recipe_line('NL004', 0.18); recipe_line('NL005', 0.01); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP010'); recipe_line('NL001', 0.03); recipe_line('NL005', 0.01); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);

    begin_recipe('SP011'); recipe_line('NL008', 0.01); recipe_line('NL006', 0.04); recipe_line('NL018', 0.02); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP012'); recipe_line('NL008', 0.01); recipe_line('NL007', 0.04); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP013'); recipe_line('NL009', 0.01); recipe_line('NL016', 0.04); recipe_line('NL011', 0.03); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP014'); recipe_line('NL008', 0.01); recipe_line('NL017', 0.03); recipe_line('NL005', 0.01); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP015'); recipe_line('NL008', 0.01); recipe_line('NL011', 0.03); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP016'); recipe_line('NL008', 0.01); recipe_line('NL003', 0.07); recipe_line('NL004', 0.10); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP017'); recipe_line('NL008', 0.01); recipe_line('NL003', 0.07); recipe_line('NL004', 0.10); recipe_line('NL012', 0.05); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP018'); recipe_line('NL028', 0.01); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP019'); recipe_line('NL010', 0.02); recipe_line('NL004', 0.15); recipe_line('NL005', 0.02); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);
    begin_recipe('SP020'); recipe_line('NL008', 0.01); recipe_line('NL011', 0.04); recipe_line('NL005', 0.01); recipe_line('NL024', 0.01); recipe_line('NL025', 0.01); recipe_line('NL026', 0.01);

    begin_recipe('SP021'); recipe_line('NL020', 0.08); recipe_line('NL021', 0.04); recipe_line('NL022', 0.05); recipe_line('NL027', 0.01);
    begin_recipe('SP022'); recipe_line('NL020', 0.06); recipe_line('NL021', 0.02); recipe_line('NL022', 0.05); recipe_line('NL027', 0.01);
    begin_recipe('SP023'); recipe_line('NL020', 0.05); recipe_line('NL022', 0.05); recipe_line('NL023', 0.04); recipe_line('NL019', 0.02); recipe_line('NL001', 0.01); recipe_line('NL027', 0.01);
    begin_recipe('SP024'); recipe_line('NL020', 0.04); recipe_line('NL021', 0.02); recipe_line('NL022', 0.05); recipe_line('NL023', 0.05); recipe_line('NL027', 0.01);
    begin_recipe('SP025'); recipe_line('NL020', 0.06); recipe_line('NL021', 0.03); recipe_line('NL022', 0.05); recipe_line('NL019', 0.02); recipe_line('NL027', 0.01);
    begin_recipe('SP026'); recipe_line('NL020', 0.07); recipe_line('NL021', 0.02); recipe_line('NL022', 0.05); recipe_line('NL027', 0.01);
    begin_recipe('SP027'); recipe_line('NL020', 0.07); recipe_line('NL021', 0.02); recipe_line('NL022', 0.05); recipe_line('NL019', 0.02); recipe_line('NL027', 0.01);
    begin_recipe('SP028'); recipe_line('NL022', 0.05); recipe_line('NL004', 0.12); recipe_line('NL003', 0.08); recipe_line('NL027', 0.01);
    begin_recipe('SP029'); recipe_line('NL020', 0.05); recipe_line('NL021', 0.02); recipe_line('NL022', 0.05); recipe_line('NL023', 0.04); recipe_line('NL027', 0.01);
    begin_recipe('SP030'); recipe_line('NL020', 0.07); recipe_line('NL021', 0.02); recipe_line('NL022', 0.05); recipe_line('NL005', 0.01); recipe_line('NL027', 0.01);
END;
/

COMMIT;

PROMPT ==================================================
PROMPT 11. INSERT TON_KHO
PROMPT ==================================================

DECLARE
    FUNCTION material_id(p_index NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN 'NL' || LPAD(p_index, 3, '0');
    END;

    FUNCTION stock_quantity(p_branch NUMBER, p_material_id VARCHAR2) RETURN NUMBER IS
    BEGIN
        RETURN CASE p_material_id
            WHEN 'NL001' THEN 25 + p_branch * 5
            WHEN 'NL002' THEN CASE WHEN p_branch = 1 THEN 0.06 ELSE 8 + p_branch * 2 END
            WHEN 'NL003' THEN 35 + p_branch * 6
            WHEN 'NL004' THEN 80 + p_branch * 15
            WHEN 'NL005' THEN 25 + p_branch * 4
            WHEN 'NL006' THEN 18 + p_branch * 3
            WHEN 'NL007' THEN 16 + p_branch * 3
            WHEN 'NL008' THEN 18 + p_branch * 3
            WHEN 'NL009' THEN 12 + p_branch * 2
            WHEN 'NL010' THEN 10 + p_branch * 2
            WHEN 'NL011' THEN 12 + p_branch * 2
            WHEN 'NL012' THEN 18 + p_branch * 3
            WHEN 'NL013' THEN 15 + p_branch * 2
            WHEN 'NL014' THEN 12 + p_branch * 2
            WHEN 'NL015' THEN 8 + p_branch
            WHEN 'NL016' THEN 12 + p_branch * 2
            WHEN 'NL017' THEN 10 + p_branch * 2
            WHEN 'NL018' THEN 8 + p_branch
            WHEN 'NL019' THEN 9 + p_branch
            WHEN 'NL020' THEN 45 + p_branch * 6
            WHEN 'NL021' THEN 25 + p_branch * 4
            WHEN 'NL022' THEN 30 + p_branch * 5
            WHEN 'NL023' THEN 40 + p_branch * 5
            WHEN 'NL024' THEN 20 + p_branch * 3
            WHEN 'NL025' THEN 20 + p_branch * 3
            WHEN 'NL026' THEN 25 + p_branch * 3
            WHEN 'NL027' THEN 12 + p_branch * 2
            WHEN 'NL028' THEN 12 + p_branch * 2
            ELSE 0
        END;
    END;

    FUNCTION min_stock(p_material_id VARCHAR2) RETURN NUMBER IS
    BEGIN
        RETURN CASE p_material_id
            WHEN 'NL001' THEN 5
            WHEN 'NL002' THEN 0.5
            WHEN 'NL003' THEN 10
            WHEN 'NL004' THEN 30
            WHEN 'NL005' THEN 8
            WHEN 'NL006' THEN 6
            WHEN 'NL007' THEN 6
            WHEN 'NL008' THEN 5
            WHEN 'NL009' THEN 4
            WHEN 'NL010' THEN 3
            WHEN 'NL011' THEN 3
            WHEN 'NL012' THEN 6
            WHEN 'NL013' THEN 5
            WHEN 'NL014' THEN 4
            WHEN 'NL015' THEN 2
            WHEN 'NL016' THEN 4
            WHEN 'NL017' THEN 3
            WHEN 'NL018' THEN 2
            WHEN 'NL019' THEN 2
            WHEN 'NL020' THEN 12
            WHEN 'NL021' THEN 8
            WHEN 'NL022' THEN 5
            WHEN 'NL023' THEN 10
            WHEN 'NL024' THEN 5
            WHEN 'NL025' THEN 5
            WHEN 'NL026' THEN 5
            WHEN 'NL027' THEN 3
            WHEN 'NL028' THEN 4
            ELSE 0
        END;
    END;

BEGIN
    FOR b IN 1..8 LOOP
        FOR m IN 1..28 LOOP
            DECLARE
                v_material_id VARCHAR2(10);
                v_stock_qty   NUMBER(12,2);
                v_min_stock   NUMBER(12,2);
            BEGIN
                v_material_id := material_id(m);
                v_stock_qty := stock_quantity(b, v_material_id);
                v_min_stock := min_stock(v_material_id);

                INSERT INTO ton_kho (
                    kho_id,
                    san_pham_id,
                    so_luong_ton,
                    muc_ton_toi_thieu,
                    last_updated
                )
                VALUES (
                    'KHO' || LPAD(b, 3, '0'),
                    v_material_id,
                    v_stock_qty,
                    v_min_stock,
                    SYSTIMESTAMP
                );
            END;
        END LOOP;
    END LOOP;
END;
/
COMMIT;

PROMPT ==================================================
PROMPT 12. INSERT KHACH_HANG
PROMPT ==================================================

DECLARE
    TYPE name_table IS TABLE OF NVARCHAR2(60) INDEX BY PLS_INTEGER;
    v_ho name_table;
    v_dem name_table;
    v_ten name_table;

    FUNCTION demo_name(p_seed NUMBER) RETURN NVARCHAR2 IS
    BEGIN
        RETURN v_ho(MOD(p_seed - 1, 10) + 1)
            || N' '
            || v_dem(MOD(TRUNC((p_seed - 1) / 10), 10) + 1)
            || N' '
            || v_ten(MOD(TRUNC((p_seed - 1) / 100), 10) + 1);
    END;

BEGIN
    v_ho(1) := N'Nguyễn'; v_ho(2) := N'Trần'; v_ho(3) := N'Lê'; v_ho(4) := N'Phạm'; v_ho(5) := N'Hoàng';
    v_ho(6) := N'Phan'; v_ho(7) := N'Võ'; v_ho(8) := N'Đặng'; v_ho(9) := N'Bùi'; v_ho(10) := N'Đỗ';

    v_dem(1) := N'Minh'; v_dem(2) := N'Thanh'; v_dem(3) := N'Gia'; v_dem(4) := N'Hoài'; v_dem(5) := N'Quang';
    v_dem(6) := N'Ngọc'; v_dem(7) := N'Phương'; v_dem(8) := N'Khánh'; v_dem(9) := N'Tuấn'; v_dem(10) := N'Bảo';

    v_ten(1) := N'An'; v_ten(2) := N'Bình'; v_ten(3) := N'Chi'; v_ten(4) := N'Duy'; v_ten(5) := N'Hà';
    v_ten(6) := N'Khang'; v_ten(7) := N'Linh'; v_ten(8) := N'Nam'; v_ten(9) := N'Trúc'; v_ten(10) := N'Vy';

    FOR i IN 1..80 LOOP
        DECLARE
            v_customer_name NVARCHAR2(120);
        BEGIN
            v_customer_name := demo_name(700 + i);

            INSERT INTO khach_hang (
                khach_hang_id,
                ho_ten,
                phone,
                email,
                hang_thanh_vien,
                diem_tich_luy,
                ghi_chu,
                created_at,
                updated_at
            )
            VALUES (
                'KH' || LPAD(i, 8, '0'),
                v_customer_name,
                '08' || LPAD(i, 8, '0'),
                'khach' || LPAD(i, 3, '0') || '@phungloccoffee.demo',
                CASE
                    WHEN MOD(i, 20) = 0 THEN 'KIM_CUONG'
                    WHEN MOD(i, 10) = 0 THEN 'VANG'
                    WHEN MOD(i, 5) = 0 THEN 'BAC'
                    ELSE 'THUONG'
                END,
                MOD(i * 37, 1000),
                N'Khách hàng demo',
                TIMESTAMP '2026-05-27 08:00:00',
                SYSTIMESTAMP
            );
        END;
    END LOOP;
END;
/

COMMIT;

PROMPT ==================================================
PROMPT 13. INSERT DON_HANG VA CHI_TIET_DON_HANG
PROMPT ==================================================

DECLARE
    v_don_hang_id     VARCHAR2(10);
    v_ctdh_id         VARCHAR2(10);
    v_nhan_vien_id    VARCHAR2(50);
    v_stt_cn          NUMBER := 0;
    v_count_dh        NUMBER := 0;
    v_count_ctdh      NUMBER := 0;
BEGIN
    -- Xóa dữ liệu demo cũ nếu có
    DELETE FROM chi_tiet_don_hang
    WHERE don_hang_id LIKE 'DH%'
       OR chi_tiet_don_hang_id LIKE 'CTDH%'
       OR chi_tiet_don_hang_id LIKE 'C%';

    DELETE FROM don_hang
    WHERE don_hang_id LIKE 'DH%'
       OR don_hang_id LIKE 'D%';

    FOR cn IN (
        SELECT chi_nhanh_id
        FROM chi_nhanh
        ORDER BY chi_nhanh_id
    ) LOOP

        v_stt_cn := v_stt_cn + 1;

        SELECT MIN(nhan_vien_id)
        INTO v_nhan_vien_id
        FROM nhan_vien
        WHERE chi_nhanh_id = cn.chi_nhanh_id;

        IF v_nhan_vien_id IS NULL THEN
            SELECT MIN(nhan_vien_id)
            INTO v_nhan_vien_id
            FROM nhan_vien;
        END IF;

        IF v_nhan_vien_id IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Chua co du lieu trong bang NHAN_VIEN.');
        END IF;

        FOR i IN 1..10 LOOP

            -- Ví dụ: DH01001, DH01002...
            v_don_hang_id := 'DH' || LPAD(v_stt_cn, 2, '0') || LPAD(i, 3, '0');

            INSERT INTO don_hang (
                don_hang_id,
                chi_nhanh_id,
                nhan_vien_id,
                tong_tien
            )
            VALUES (
                v_don_hang_id,
                cn.chi_nhanh_id,
                v_nhan_vien_id,
                0
            );

            v_count_dh := v_count_dh + 1;

            FOR sp IN (
                SELECT san_pham_id, gia_ban, ROWNUM AS rn
                FROM san_pham
                WHERE ROWNUM <= 3
            ) LOOP

                -- Tối đa 9 ký tự.
                -- Ví dụ: CTDH01011
                -- CTDH + mã CN 2 số + mã đơn 2 số + STT sản phẩm 1 số
                v_ctdh_id :=
                    'CTDH'
                    || LPAD(v_stt_cn, 2, '0')
                    || LPAD(i, 2, '0')
                    || TO_CHAR(sp.rn);

                INSERT INTO chi_tiet_don_hang (
                    chi_tiet_don_hang_id,
                    don_hang_id,
                    san_pham_id,
                    so_luong,
                    don_gia,
                    thanh_tien
                )
                VALUES (
                    v_ctdh_id,
                    v_don_hang_id,
                    sp.san_pham_id,
                    1,
                    sp.gia_ban,
                    sp.gia_ban
                );

                v_count_ctdh := v_count_ctdh + 1;

            END LOOP;

            UPDATE don_hang
            SET tong_tien = (
                SELECT NVL(SUM(thanh_tien), 0)
                FROM chi_tiet_don_hang
                WHERE don_hang_id = v_don_hang_id
            )
            WHERE don_hang_id = v_don_hang_id;

        END LOOP;
    END LOOP;

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Da insert DON_HANG: ' || v_count_dh);
    DBMS_OUTPUT.PUT_LINE('Da insert CHI_TIET_DON_HANG: ' || v_count_ctdh);

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Loi section 13: ' || SQLERRM);
        RAISE;
END;
/
COMMIT;

PROMPT ==================================================
PROMPT 14. INSERT PHIEU_XUAT_KHO
PROMPT ==================================================

DECLARE
    v_day DATE;
BEGIN
    FOR b IN 1..8 LOOP
        FOR d IN 0..6 LOOP
            v_day := DATE '2026-05-27' + d;

            INSERT INTO phieu_xuat_kho (
                phieu_xuat_id,
                kho_id,
                nhan_vien_id,
                ngay_xuat,
                ly_do_xuat,
                so_luong_mat_hang,
                tong_tien,
                trang_thai,
                ghi_chu,
                created_at,
                updated_at
            )
            VALUES (
                'PXD' || LPAD(b, 3, '0') || TO_CHAR(v_day, 'MMDD'),
                'KHO' || LPAD(b, 3, '0'),
                'NVK' || LPAD(b, 3, '0') || '01',
                FROM_TZ(CAST(v_day AS TIMESTAMP), '+07:00') + NUMTODSINTERVAL(23, 'HOUR'),
                N'Tự động trừ kho theo đơn POS demo',
                0,
                0,
                'DA_DUYET',
                N'Phiếu xuất tổng hợp từ hóa đơn đã thanh toán ngày ' || TO_CHAR(v_day, 'DD/MM/YYYY'),
                SYSTIMESTAMP,
                SYSTIMESTAMP
            );
        END LOOP;
    END LOOP;
END;
/

COMMIT;

PROMPT ==================================================
PROMPT 15. INSERT CHI_TIET_XUAT_KHO TU DINH_MUC
PROMPT ==================================================

INSERT INTO chi_tiet_xuat_kho (
    phieu_xuat_id,
    san_pham_id,
    so_luong,
    don_vi_tinh,
    ghi_chu,
    created_at,
    updated_at
)
SELECT phieu_xuat_id,
       san_pham_nguyen_lieu,
       so_luong,
       don_vi_tinh,
       N'Xuất kho tự động theo định mức POS demo',
       SYSTIMESTAMP,
       SYSTIMESTAMP
FROM (
    SELECT 'PXD'
           || SUBSTR(dh.chi_nhanh_id, 3, 3)
           || TO_CHAR(TRUNC(CAST(dh.created_at AS DATE)), 'MMDD') AS phieu_xuat_id,
           ctdm.san_pham_nguyen_lieu,
           SUM(ct.so_luong * ctdm.so_luong_dung) AS so_luong,
           MAX(ctdm.don_vi_tinh) AS don_vi_tinh
    FROM don_hang dh
    JOIN chi_tiet_don_hang ct
      ON ct.don_hang_id = dh.don_hang_id
    JOIN dinh_muc_san_pham dm
      ON dm.san_pham_ban_id = ct.san_pham_id
     AND dm.trang_thai = 1
    JOIN chi_tiet_dinh_muc ctdm
      ON ctdm.dinh_muc_id = dm.dinh_muc_id
    WHERE dh.don_hang_id LIKE 'DHD%'
      AND dh.trang_thai = 'DA_HOAN_THANH'
      AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
    GROUP BY 'PXD'
             || SUBSTR(dh.chi_nhanh_id, 3, 3)
             || TO_CHAR(TRUNC(CAST(dh.created_at AS DATE)), 'MMDD'),
             ctdm.san_pham_nguyen_lieu
);

COMMIT;

PROMPT ==================================================
PROMPT 16. CAP NHAT PHIEU_XUAT_KHO
PROMPT ==================================================

MERGE INTO phieu_xuat_kho px
USING (
    SELECT ctxk.phieu_xuat_id,
           COUNT(*) AS so_luong_mat_hang,
           SUM(ctxk.so_luong * sp.gia_von) AS tong_tien
    FROM chi_tiet_xuat_kho ctxk
    JOIN san_pham sp
      ON sp.san_pham_id = ctxk.san_pham_id
    WHERE ctxk.phieu_xuat_id LIKE 'PXD%'
    GROUP BY ctxk.phieu_xuat_id
) agg
ON (px.phieu_xuat_id = agg.phieu_xuat_id)
WHEN MATCHED THEN UPDATE
SET px.so_luong_mat_hang = agg.so_luong_mat_hang,
    px.tong_tien = agg.tong_tien,
    px.updated_at = SYSTIMESTAMP;

COMMIT;

PROMPT ==================================================
PROMPT 17. KIEM TRA KET QUA SEED
PROMPT ==================================================

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM chi_nhanh WHERE REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$');
    DBMS_OUTPUT.PUT_LINE('Tong chi nhanh demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM nhan_vien WHERE nhan_vien_id IN ('NVCEO', 'NVIT') OR nhan_vien_id LIKE 'NVQL%' OR nhan_vien_id LIKE 'NVT%' OR nhan_vien_id LIKE 'NVK%' OR nhan_vien_id LIKE 'NVPC%' OR nhan_vien_id LIKE 'NVPV%';
    DBMS_OUTPUT.PUT_LINE('Tong nhan vien demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM app_user WHERE user_id IN ('UCEO', 'UIT') OR user_id LIKE 'UQL%' OR user_id LIKE 'UTN%' OR user_id LIKE 'UKHO%';
    DBMS_OUTPUT.PUT_LINE('Tong tai khoan demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM san_pham WHERE REGEXP_LIKE(san_pham_id, '^SP0(0[1-9]|[1-2][0-9]|30)$');
    DBMS_OUTPUT.PUT_LINE('Tong san pham ban demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM san_pham WHERE REGEXP_LIKE(san_pham_id, '^NL0(0[1-9]|1[0-9]|2[0-8])$');
    DBMS_OUTPUT.PUT_LINE('Tong nguyen lieu demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM dinh_muc_san_pham WHERE dinh_muc_id LIKE 'DMD%';
    DBMS_OUTPUT.PUT_LINE('Tong dinh muc demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM ton_kho WHERE REGEXP_LIKE(kho_id, '^KHO00[1-8]$');
    DBMS_OUTPUT.PUT_LINE('Tong dong ton kho demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM don_hang WHERE don_hang_id LIKE 'DHD%';
    DBMS_OUTPUT.PUT_LINE('Tong don hang demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM chi_tiet_don_hang WHERE don_hang_id LIKE 'DHD%';
    DBMS_OUTPUT.PUT_LINE('Tong chi tiet don hang demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM phieu_xuat_kho WHERE phieu_xuat_id LIKE 'PXD%';
    DBMS_OUTPUT.PUT_LINE('Tong phieu xuat kho demo: ' || v_count);

    SELECT COUNT(*) INTO v_count FROM chi_tiet_xuat_kho WHERE phieu_xuat_id LIKE 'PXD%';
    DBMS_OUTPUT.PUT_LINE('Tong chi tiet xuat kho demo: ' || v_count);

    DBMS_OUTPUT.PUT_LINE('Canh bao ton kho can thay: CN001 / KHO001 / NL002 = 0.06 KG, muc toi thieu 0.5 KG.');
END;
/

PROMPT --------------------------------------------------
PROMPT Tong chi nhanh demo
SELECT COUNT(*) AS tong_chi_nhanh_demo
FROM chi_nhanh
WHERE REGEXP_LIKE(chi_nhanh_id, '^CN00[1-8]$');

PROMPT --------------------------------------------------
PROMPT Tong nhan vien demo
SELECT COUNT(*) AS tong_nhan_vien_demo
FROM nhan_vien
WHERE nhan_vien_id IN ('NVCEO', 'NVIT')
   OR nhan_vien_id LIKE 'NVQL%'
   OR nhan_vien_id LIKE 'NVT%'
   OR nhan_vien_id LIKE 'NVK%'
   OR nhan_vien_id LIKE 'NVPC%'
   OR nhan_vien_id LIKE 'NVPV%';

PROMPT --------------------------------------------------
PROMPT Tong tai khoan demo
SELECT COUNT(*) AS tong_tai_khoan_demo
FROM app_user
WHERE user_id IN ('UCEO', 'UIT')
   OR user_id LIKE 'UQL%'
   OR user_id LIKE 'UTN%'
   OR user_id LIKE 'UKHO%';

PROMPT --------------------------------------------------
PROMPT Tong san pham ban demo
SELECT COUNT(*) AS tong_san_pham_ban
FROM san_pham
WHERE REGEXP_LIKE(san_pham_id, '^SP0(0[1-9]|[1-2][0-9]|30)$')
  AND loai_san_pham IN ('THANH_PHAM', 'BAN_THANH_PHAM');

PROMPT --------------------------------------------------
PROMPT Tong nguyen lieu demo
SELECT COUNT(*) AS tong_nguyen_lieu
FROM san_pham
WHERE REGEXP_LIKE(san_pham_id, '^NL0(0[1-9]|1[0-9]|2[0-8])$')
  AND loai_san_pham = 'NGUYEN_LIEU';

PROMPT --------------------------------------------------
PROMPT Tong dinh muc demo
SELECT COUNT(*) AS tong_dinh_muc
FROM dinh_muc_san_pham
WHERE dinh_muc_id LIKE 'DMD%';

PROMPT --------------------------------------------------
PROMPT Tong ton kho demo
SELECT COUNT(*) AS tong_ton_kho
FROM ton_kho
WHERE REGEXP_LIKE(kho_id, '^KHO00[1-8]$');

PROMPT --------------------------------------------------
PROMPT Tong don hang demo
SELECT COUNT(*) AS tong_don_hang
FROM don_hang
WHERE don_hang_id LIKE 'DHD%';

PROMPT --------------------------------------------------
PROMPT Tong chi tiet don hang demo
SELECT COUNT(*) AS tong_chi_tiet_don_hang
FROM chi_tiet_don_hang
WHERE don_hang_id LIKE 'DHD%';

PROMPT --------------------------------------------------
PROMPT Tong phieu xuat kho demo
SELECT COUNT(*) AS tong_phieu_xuat_kho
FROM phieu_xuat_kho
WHERE phieu_xuat_id LIKE 'PXD%';

PROMPT --------------------------------------------------
PROMPT Tong chi tiet xuat kho demo
SELECT COUNT(*) AS tong_chi_tiet_xuat_kho
FROM chi_tiet_xuat_kho
WHERE phieu_xuat_id LIKE 'PXD%';

PROMPT --------------------------------------------------
PROMPT So don hang tung chi nhanh
SELECT cn.chi_nhanh_id,
       cn.ten_chi_nhanh,
       COUNT(dh.don_hang_id) AS so_don_hang,
       SUM(CASE WHEN dh.trang_thai = 'DA_HOAN_THANH' AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN' THEN dh.tong_tien ELSE 0 END) AS doanh_thu_da_thanh_toan
FROM chi_nhanh cn
LEFT JOIN don_hang dh
  ON dh.chi_nhanh_id = cn.chi_nhanh_id
 AND dh.don_hang_id LIKE 'DHD%'
WHERE REGEXP_LIKE(cn.chi_nhanh_id, '^CN00[1-8]$')
GROUP BY cn.chi_nhanh_id, cn.ten_chi_nhanh
ORDER BY cn.chi_nhanh_id;

PROMPT --------------------------------------------------
PROMPT So don hang tung ngay
SELECT TRUNC(CAST(created_at AS DATE)) AS ngay_ban,
       COUNT(*) AS so_don_hang,
       SUM(CASE WHEN trang_thai = 'DA_HOAN_THANH' AND trang_thai_thanh_toan = 'DA_THANH_TOAN' THEN tong_tien ELSE 0 END) AS doanh_thu_da_thanh_toan
FROM don_hang
WHERE don_hang_id LIKE 'DHD%'
GROUP BY TRUNC(CAST(created_at AS DATE))
ORDER BY ngay_ban;

PROMPT --------------------------------------------------
PROMPT Top 10 san pham ban chay
SELECT *
FROM (
    SELECT sp.san_pham_id,
           sp.ten_san_pham,
           SUM(ct.so_luong) AS so_luong_ban,
           SUM(ct.thanh_tien) AS doanh_thu
    FROM don_hang dh
    JOIN chi_tiet_don_hang ct
      ON ct.don_hang_id = dh.don_hang_id
    JOIN san_pham sp
      ON sp.san_pham_id = ct.san_pham_id
    WHERE dh.don_hang_id LIKE 'DHD%'
      AND dh.trang_thai = 'DA_HOAN_THANH'
      AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN'
    GROUP BY sp.san_pham_id, sp.ten_san_pham
    ORDER BY so_luong_ban DESC, doanh_thu DESC
)
WHERE ROWNUM <= 10;

PROMPT --------------------------------------------------
PROMPT Nguyen lieu duoi muc toi thieu
SELECT cn.chi_nhanh_id,
       cn.ten_chi_nhanh,
       k.kho_id,
       sp.san_pham_id,
       sp.ten_san_pham,
       sp.don_vi_tinh,
       tk.so_luong_ton,
       tk.muc_ton_toi_thieu
FROM ton_kho tk
JOIN kho k
  ON k.kho_id = tk.kho_id
JOIN chi_nhanh cn
  ON cn.chi_nhanh_id = k.chi_nhanh_id
JOIN san_pham sp
  ON sp.san_pham_id = tk.san_pham_id
WHERE tk.so_luong_ton < tk.muc_ton_toi_thieu
  AND REGEXP_LIKE(k.kho_id, '^KHO00[1-8]$')
ORDER BY cn.chi_nhanh_id, sp.san_pham_id;

PROMPT ==================================================
PROMPT HOAN TAT TAO DU LIEU MAU PHUNG LOC COFFEE
PROMPT ==================================================
SELECT COUNT(*) FROM chi_nhanh;
SELECT COUNT(*) FROM nhan_vien;
SELECT COUNT(*) FROM san_pham;
PROMPT ==================================================
PROMPT 13. INSERT DON_HANG VA CHI_TIET_DON_HANG
PROMPT ==================================================

DECLARE
    v_don_hang_id    VARCHAR2(10);
    v_ctdh_id        VARCHAR2(10);
    v_nhan_vien_id   VARCHAR2(50);
    v_stt_cn         NUMBER := 0;
BEGIN
    FOR cn IN (
        SELECT chi_nhanh_id
        FROM chi_nhanh
        ORDER BY chi_nhanh_id
    ) LOOP

        v_stt_cn := v_stt_cn + 1;

        SELECT MIN(nhan_vien_id)
        INTO v_nhan_vien_id
        FROM nhan_vien
        WHERE chi_nhanh_id = cn.chi_nhanh_id;

        IF v_nhan_vien_id IS NULL THEN
            SELECT MIN(nhan_vien_id)
            INTO v_nhan_vien_id
            FROM nhan_vien;
        END IF;

        IF v_nhan_vien_id IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Chua co du lieu trong bang NHAN_VIEN.');
        END IF;

        FOR i IN 1..10 LOOP

            v_don_hang_id := 'D' || LPAD(v_stt_cn, 2, '0') || LPAD(i, 3, '0');

            INSERT INTO don_hang (
                don_hang_id,
                chi_nhanh_id,
                nhan_vien_id,
                tong_tien
            )
            VALUES (
                v_don_hang_id,
                cn.chi_nhanh_id,
                v_nhan_vien_id,
                0
            );

            FOR sp IN (
                SELECT san_pham_id, gia_ban, ROWNUM AS rn
                FROM san_pham
                WHERE ROWNUM <= 3
            ) LOOP

                v_ctdh_id :=
                    'C'
                    || LPAD(v_stt_cn, 2, '0')
                    || LPAD(i, 2, '0')
                    || LPAD(sp.rn, 2, '0');

                INSERT INTO chi_tiet_don_hang (
                    chi_tiet_don_hang_id,
                    don_hang_id,
                    san_pham_id,
                    so_luong,
                    don_gia,
                    thanh_tien
                )
                VALUES (
                    v_ctdh_id,
                    v_don_hang_id,
                    sp.san_pham_id,
                    1,
                    sp.gia_ban,
                    sp.gia_ban
                );

            END LOOP;

            UPDATE don_hang
            SET tong_tien = (
                SELECT NVL(SUM(thanh_tien), 0)
                FROM chi_tiet_don_hang
                WHERE don_hang_id = v_don_hang_id
            )
            WHERE don_hang_id = v_don_hang_id;

        END LOOP;
    END LOOP;

    COMMIT;
END;
/

SET SERVEROUTPUT ON;

DECLARE
    v_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE(RPAD('TEN_BANG', 40) || ' | ' || RPAD('SO_DONG', 10) || ' | TRANG_THAI');
    DBMS_OUTPUT.PUT_LINE(RPAD('-', 40, '-') || '-+-' || RPAD('-', 10, '-') || '-+-' || RPAD('-', 15, '-'));

    FOR t IN (
        SELECT table_name
        FROM user_tables
        ORDER BY table_name
    ) LOOP
        EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || t.table_name INTO v_count;

        DBMS_OUTPUT.PUT_LINE(
            RPAD(t.table_name, 40) || ' | ' ||
            RPAD(v_count, 10) || ' | ' ||
            CASE 
                WHEN v_count = 0 THEN 'KHONG CO DU LIEU'
                ELSE 'CO DU LIEU'
            END
        );
    END LOOP;
END;
/

UPDATE
    don_hang
SET
    trang_thai_thanh_toan='DA_THANH_TOAN';
UPDATE
    don_hang
SET
    trang_thai='DA_HOAN_THANH';
COMMIT;