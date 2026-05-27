CREATE TABLE chi_nhanh (
    chi_nhanh_id VARCHAR2 (20) PRIMARY KEY,
    ten_chi_nhanh NVARCHAR2 (150) NOT NULL,
    dia_chi NVARCHAR2 (255),
    phone VARCHAR2 (20),
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_chi_nhanh_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE app_user (
    user_id VARCHAR2 (20) PRIMARY KEY,
    ten_dang_nhap VARCHAR2 (50) NOT NULL UNIQUE,
    mat_khau VARCHAR2 (255) NOT NULL,
    vai_tro VARCHAR2 (30) NOT NULL,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
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

CREATE TABLE nhan_vien (
    nhan_vien_id VARCHAR2 (20) PRIMARY KEY,
    user_id VARCHAR2 (20) UNIQUE,
    chi_nhanh_id VARCHAR2 (20),
    ho_ten NVARCHAR2 (150) NOT NULL,
    cccd VARCHAR2 (20) UNIQUE,
    email VARCHAR2 (120) UNIQUE,
    phone VARCHAR2 (20) UNIQUE,
    chuc_vu VARCHAR2 (30) NOT NULL,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    ngay_vao_lam DATE,
    ghi_chu NVARCHAR2 (500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_nv_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_nv_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh (chi_nhanh_id),
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

CREATE TABLE khach_hang (
    khach_hang_id VARCHAR2 (20) PRIMARY KEY,
    ho_ten NVARCHAR2 (150) NOT NULL,
    phone VARCHAR2 (20) UNIQUE,
    email VARCHAR2 (120) UNIQUE,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_kh_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE pos_device (
    pos_device_id VARCHAR2 (20) PRIMARY KEY,
    chi_nhanh_id VARCHAR2 (20) NOT NULL,
    ten_thiet_bi NVARCHAR2 (150) NOT NULL,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_pos_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh (chi_nhanh_id),
    CONSTRAINT ck_pos_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE kho (
    kho_id VARCHAR2 (20) PRIMARY KEY,
    chi_nhanh_id VARCHAR2 (20) NOT NULL,
    ten_kho NVARCHAR2 (150) NOT NULL,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_kho_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh (chi_nhanh_id),
    CONSTRAINT ck_kho_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE nha_cung_cap (
    nha_cung_cap_id VARCHAR2 (20) PRIMARY KEY,
    ten_nha_cung_cap NVARCHAR2 (150) NOT NULL,
    phone VARCHAR2 (20),
    email VARCHAR2 (120),
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_ncc_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE danh_muc_san_pham (
    danh_muc_id VARCHAR2 (20) PRIMARY KEY,
    ten_danh_muc NVARCHAR2 (150) NOT NULL,
    mo_ta NVARCHAR2 (500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE san_pham (
    san_pham_id VARCHAR2 (20) PRIMARY KEY,
    danh_muc_id VARCHAR2 (20),
    ten_san_pham NVARCHAR2 (150) NOT NULL,
    loai_san_pham VARCHAR2 (30) NOT NULL,
    don_vi_tinh VARCHAR2 (10) NOT NULL,
    gia_ban NUMBER (12, 2) DEFAULT 0 NOT NULL,
    gia_von NUMBER (12, 2) DEFAULT 0 NOT NULL,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_sp_danh_muc FOREIGN KEY (danh_muc_id) REFERENCES danh_muc_san_pham (danh_muc_id),
    CONSTRAINT ck_sp_loai CHECK (
        loai_san_pham IN (
            'THANH_PHAM',
            'NGUYEN_LIEU',
            'BAN_THANH_PHAM',
            'TOPPING'
        )
    ),
    CONSTRAINT ck_sp_dvt CHECK (
        don_vi_tinh IN ('ML', 'L', 'MG', 'KG')
    ),
    CONSTRAINT ck_sp_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE ton_kho (
    kho_id VARCHAR2 (20) NOT NULL,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong_ton NUMBER (12, 2) DEFAULT 0 NOT NULL,
    muc_ton_toi_thieu NUMBER (12, 2) DEFAULT 0 NOT NULL,
    last_updated TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_ton_kho PRIMARY KEY (kho_id, san_pham_id),
    CONSTRAINT fk_tk_kho FOREIGN KEY (kho_id) REFERENCES kho (kho_id),
    CONSTRAINT fk_tk_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE don_hang (
    don_hang_id VARCHAR2 (30) PRIMARY KEY,
    khach_hang_id VARCHAR2 (20),
    chi_nhanh_id VARCHAR2 (20),
    nhan_vien_id VARCHAR2 (20),
    trang_thai VARCHAR2 (30) NOT NULL,
    tam_tinh NUMBER (12, 2) DEFAULT 0 NOT NULL,
    giam_gia NUMBER (12, 2) DEFAULT 0 NOT NULL,
    tong_tien NUMBER (12, 2) DEFAULT 0 NOT NULL,
    trang_thai_thanh_toan VARCHAR2 (30) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_dh_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang (khach_hang_id),
    CONSTRAINT fk_dh_chi_nhanh FOREIGN KEY (chi_nhanh_id) REFERENCES chi_nhanh (chi_nhanh_id),
    CONSTRAINT fk_dh_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien (nhan_vien_id),
    CONSTRAINT ck_dh_trang_thai CHECK (
        trang_thai IN (
            'DANG_TAO',
            'DA_HOAN_THANH',
            'DA_HUY'
        )
    ),
    CONSTRAINT ck_dh_tt_thanhtoan CHECK (
        trang_thai_thanh_toan IN (
            'CHUA_THANH_TOAN',
            'DA_THANH_TOAN',
            'DA_HOAN_TIEN'
        )
    )
);

CREATE TABLE chi_tiet_don_hang (
    chi_tiet_don_hang_id VARCHAR2 (30) PRIMARY KEY,
    don_hang_id VARCHAR2 (30) NOT NULL,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong NUMBER (12, 2) NOT NULL,
    don_gia NUMBER (12, 2) NOT NULL,
    thanh_tien NUMBER (12, 2) NOT NULL,
    ghi_chu NVARCHAR2 (500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_ctdh_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang (don_hang_id),
    CONSTRAINT fk_ctdh_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE order_detail_topping (
    id VARCHAR2 (30) PRIMARY KEY,
    order_detail_id VARCHAR2 (30) NOT NULL,
    topping_product_id VARCHAR2 (20) NOT NULL,
    topping_name NVARCHAR2 (150) NOT NULL,
    quantity NUMBER (12, 2) NOT NULL,
    unit_price NUMBER (12, 2) NOT NULL,
    subtotal NUMBER (12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_odt_order_detail FOREIGN KEY (order_detail_id) REFERENCES chi_tiet_don_hang (chi_tiet_don_hang_id),
    CONSTRAINT fk_odt_topping_product FOREIGN KEY (topping_product_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE giao_dich_offline (
    giao_dich_id VARCHAR2 (30) PRIMARY KEY,
    pos_device_id VARCHAR2 (20),
    payload CLOB,
    trang_thai NUMBER (1) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_gdo_pos FOREIGN KEY (pos_device_id) REFERENCES pos_device (pos_device_id),
    CONSTRAINT ck_gdo_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE phieu_nhap_kho (
    phieu_nhap_id VARCHAR2 (30) PRIMARY KEY,
    kho_id VARCHAR2 (20) NOT NULL,
    nha_cung_cap_id VARCHAR2 (20),
    nhan_vien_id VARCHAR2 (20),
    ngay_nhap TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    tong_so_luong NUMBER (12, 2) DEFAULT 0 NOT NULL,
    so_luong_mat_hang NUMBER (10) DEFAULT 0 NOT NULL,
    trang_thai VARCHAR2 (20) NOT NULL,
    ghi_chu NVARCHAR2 (500),
    CONSTRAINT fk_pnk_kho FOREIGN KEY (kho_id) REFERENCES kho (kho_id),
    CONSTRAINT fk_pnk_ncc FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap (nha_cung_cap_id),
    CONSTRAINT fk_pnk_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien (nhan_vien_id),
    CONSTRAINT ck_pnk_trang_thai CHECK (
        trang_thai IN ('NHAP', 'DA_DUYET', 'DA_HUY')
    )
);

CREATE TABLE chi_tiet_nhap_kho (
    chi_tiet_nhap_id VARCHAR2 (30) PRIMARY KEY,
    phieu_nhap_id VARCHAR2 (30) NOT NULL,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong NUMBER (12, 2) NOT NULL,
    don_gia NUMBER (12, 2) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_ctnk_pnk FOREIGN KEY (phieu_nhap_id) REFERENCES phieu_nhap_kho (phieu_nhap_id),
    CONSTRAINT fk_ctnk_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE phieu_xuat_kho (
    phieu_xuat_id VARCHAR2 (30) PRIMARY KEY,
    kho_id VARCHAR2 (20) NOT NULL,
    nhan_vien_id VARCHAR2 (20),
    ngay_xuat TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    ly_do_xuat NVARCHAR2 (500),
    so_luong_mat_hang NUMBER (10) DEFAULT 0 NOT NULL,
    tong_tien NUMBER (12, 2) DEFAULT 0 NOT NULL,
    trang_thai VARCHAR2 (20) NOT NULL,
    ghi_chu NVARCHAR2 (500),
    CONSTRAINT fk_pxk_kho FOREIGN KEY (kho_id) REFERENCES kho (kho_id),
    CONSTRAINT fk_pxk_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien (nhan_vien_id),
    CONSTRAINT ck_pxk_trang_thai CHECK (
        trang_thai IN ('NHAP', 'DA_DUYET', 'DA_HUY')
    )
);

CREATE TABLE chi_tiet_xuat_kho (
    chi_tiet_xuat_id VARCHAR2 (30) PRIMARY KEY,
    phieu_xuat_id VARCHAR2 (30) NOT NULL,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong NUMBER (12, 2) NOT NULL,
    don_gia NUMBER (12, 2) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_ctxk_pxk FOREIGN KEY (phieu_xuat_id) REFERENCES phieu_xuat_kho (phieu_xuat_id),
    CONSTRAINT fk_ctxk_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE phieu_dieu_chuyen_kho (
    phieu_dieu_chuyen_id VARCHAR2 (30) PRIMARY KEY,
    kho_nguon_id VARCHAR2 (20) NOT NULL,
    kho_dich_id VARCHAR2 (20) NOT NULL,
    nhan_vien_id VARCHAR2 (20),
    ngay_dieu_chuyen TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    trang_thai VARCHAR2 (20) NOT NULL,
    CONSTRAINT fk_pdc_nguon FOREIGN KEY (kho_nguon_id) REFERENCES kho (kho_id),
    CONSTRAINT fk_pdc_dich FOREIGN KEY (kho_dich_id) REFERENCES kho (kho_id),
    CONSTRAINT fk_pdc_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien (nhan_vien_id),
    CONSTRAINT ck_pdc_trang_thai CHECK (
        trang_thai IN ('NHAP', 'DA_DUYET', 'DA_HUY')
    )
);

CREATE TABLE chi_tiet_phieu_dieu_chuyen_kho (
    chi_tiet_dieu_chuyen_id VARCHAR2 (30) PRIMARY KEY,
    phieu_dieu_chuyen_id VARCHAR2 (30) NOT NULL,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong NUMBER (12, 2) NOT NULL,
    CONSTRAINT fk_ctdc_pdc FOREIGN KEY (phieu_dieu_chuyen_id) REFERENCES phieu_dieu_chuyen_kho (phieu_dieu_chuyen_id),
    CONSTRAINT fk_ctdc_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE kiem_ke_kho (
    kiem_ke_id VARCHAR2 (30) PRIMARY KEY,
    kho_id VARCHAR2 (20) NOT NULL,
    nhan_vien_id VARCHAR2 (20),
    ngay_kiem_ke TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    trang_thai VARCHAR2 (20) NOT NULL,
    ghi_chu NVARCHAR2 (500),
    CONSTRAINT fk_kk_kho FOREIGN KEY (kho_id) REFERENCES kho (kho_id),
    CONSTRAINT fk_kk_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien (nhan_vien_id),
    CONSTRAINT ck_kk_trang_thai CHECK (
        trang_thai IN (
            'NHAP',
            'DA_HOAN_THANH',
            'DA_HUY'
        )
    )
);

CREATE TABLE chi_tiet_kiem_ke_kho (
    chi_tiet_kiem_ke_id VARCHAR2 (30) PRIMARY KEY,
    kiem_ke_id VARCHAR2 (30) NOT NULL,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong_he_thong NUMBER (12, 2) DEFAULT 0 NOT NULL,
    so_luong_thuc_te NUMBER (12, 2) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_ctkk_kk FOREIGN KEY (kiem_ke_id) REFERENCES kiem_ke_kho (kiem_ke_id),
    CONSTRAINT fk_ctkk_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE dinh_muc_san_pham (
    dinh_muc_id VARCHAR2 (30) PRIMARY KEY,
    san_pham_id VARCHAR2 (20) NOT NULL,
    trang_thai NUMBER (1) DEFAULT 1 NOT NULL,
    CONSTRAINT fk_dmsp_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id),
    CONSTRAINT ck_dmsp_trang_thai CHECK (trang_thai IN (0, 1))
);

CREATE TABLE chi_tiet_dinh_muc (
    chi_tiet_dinh_muc_id VARCHAR2 (30) PRIMARY KEY,
    dinh_muc_id VARCHAR2 (30) NOT NULL,
    nguyen_lieu_id VARCHAR2 (20) NOT NULL,
    so_luong NUMBER (12, 2) NOT NULL,
    CONSTRAINT fk_ctdm_dm FOREIGN KEY (dinh_muc_id) REFERENCES dinh_muc_san_pham (dinh_muc_id),
    CONSTRAINT fk_ctdm_sp FOREIGN KEY (nguyen_lieu_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE hao_hut_nguyen_lieu (
    hao_hut_id VARCHAR2 (30) PRIMARY KEY,
    san_pham_id VARCHAR2 (20) NOT NULL,
    so_luong NUMBER (12, 2) NOT NULL,
    ly_do NVARCHAR2 (500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_hhnl_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham (san_pham_id)
);

CREATE TABLE password_reset_otp (
    otp_id VARCHAR2 (10) NOT NULL,
    user_id VARCHAR2 (10) NOT NULL,
    otp_code_hash VARCHAR2 (255) NOT NULL,
    phuong_thuc NVARCHAR2 (20) NOT NULL,
    dia_chi_nhan VARCHAR2 (255),
    reset_token VARCHAR2 (255),
    thoi_gian_het_han TIMESTAMP(6)
    WITH
        TIME ZONE NOT NULL,
        da_xac_thuc NUMBER (1) DEFAULT 0 NOT NULL,
        da_su_dung NUMBER (1) DEFAULT 0 NOT NULL,
        so_lan_sai NUMBER DEFAULT 0 NOT NULL,
        created_at TIMESTAMP(6)
    WITH
        TIME ZONE DEFAULT SYSTIMESTAMP,
        verified_at TIMESTAMP(6)
    WITH
        TIME ZONE,
        used_at TIMESTAMP(6)
    WITH
        TIME ZONE,
        CONSTRAINT pk_password_reset_otp PRIMARY KEY (otp_id),
        CONSTRAINT fk_pro_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
        CONSTRAINT ck_pro_phuong_thuc CHECK (
            phuong_thuc IN ('EMAIL', 'SMS')
        ),
        -- 0: chua xac thuc OTP
        -- 1: da xac thuc OTP
        CONSTRAINT ck_pro_da_xac_thuc CHECK (da_xac_thuc IN (0, 1)),
        -- 0: chua su dung de doi mat khau
        -- 1: da su dung de doi mat khau
        CONSTRAINT ck_pro_da_su_dung CHECK (da_su_dung IN (0, 1)),
        CONSTRAINT ck_pro_so_lan_sai CHECK (so_lan_sai >= 0)
);
