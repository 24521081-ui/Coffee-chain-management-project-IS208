package com.phungloccoffee.gui.service;

import com.phungloccoffee.gui.model.AppRole;
import com.phungloccoffee.gui.model.MenuItemModel;

import java.util.List;
import java.util.Optional;

public final class MenuConfig {
    private MenuConfig() {
    }

    public static List<MenuItemModel> getMenuByRole(AppRole role) {
        return switch (role) {
            case THU_NGAN -> List.of(
                    item("POS_MAIN", "POS Bán hàng", "▤", "POS > Bán hàng", "POS Bán hàng", "/com/phungloccoffee/gui/view/POSMainScreen.fxml"),
                    item("POS_PAYMENT", "Thanh toán", "$", "POS > Thanh toán", "Thanh toán", "/com/phungloccoffee/gui/view/PaymentScreen.fxml"),
                    item("POS_HISTORY", "Lịch sử giao dịch", "◷", "POS > Lịch sử giao dịch", "Lịch sử giao dịch", "/com/phungloccoffee/gui/view/PaymentHistory.fxml"),
                    item("CUSTOMER", "Khách hàng", "◎", "POS > Khách hàng", "Quản lý khách hàng", "/com/phungloccoffee/gui/view/customer/CustomerList.fxml"),
                    item("OFFLINE_TRANSACTION", "Giao dịch offline", "⇅", "POS > Giao dịch offline", "Giao dịch offline", "/com/phungloccoffee/gui/view/pos/OfflineTransaction.fxml")
            );
            case NHAN_VIEN_KHO -> List.of(
                    item("INVENTORY_LIST", "Tồn kho", "▦", "Kho > Tồn kho", "Tra cứu tồn kho", "/com/phungloccoffee/gui/view/inventory/InventoryList.fxml"),
                    item("WAREHOUSE_IMPORT", "Lập phiếu nhập kho", "↧", "Kho > Nhập kho", "Lập phiếu nhập kho", "/com/phungloccoffee/gui/view/WarehouseForm.fxml"),
                    item("WAREHOUSE_EXPORT", "Lập phiếu xuất kho", "↤", "Kho > Xuất kho", "Lập phiếu xuất kho", "/com/phungloccoffee/gui/view/inventory/ExportForm.fxml"),
                    item("WAREHOUSE_TRANSFER", "Điều chuyển kho", "⇄", "Kho > Điều chuyển kho", "Lập phiếu điều chuyển kho", "/com/phungloccoffee/gui/view/inventory/TransferForm.fxml"),
                    item("INVENTORY_AUDIT", "Kiểm kê kho", "▣", "Kho > Kiểm kê kho", "Lập phiếu kiểm kê kho", "/com/phungloccoffee/gui/view/InventoryAudit.fxml"),
                    item("MATERIAL_LOSS", "Hao hụt nguyên liệu", "!", "Kho > Hao hụt nguyên liệu", "Ghi nhận hao hụt nguyên liệu", "/com/phungloccoffee/gui/view/inventory/MaterialLoss.fxml"),
                    item("SUPPLIER", "Nhà cung cấp", "◇", "Kho > Nhà cung cấp", "Quản lý nhà cung cấp", "/com/phungloccoffee/gui/view/inventory/SupplierList.fxml")
            );
            case QUAN_LY_CHI_NHANH -> List.of(
                    item("BRANCH_DASHBOARD", "Dashboard chi nhánh", "⌂", "Chi nhánh > Dashboard", "Dashboard chi nhánh", "/com/phungloccoffee/gui/view/dashboard/BranchDashboard.fxml"),
                    item("APPROVE_IMPORT", "Duyệt nhập kho", "↧", "Chi nhánh > Kho > Duyệt nhập kho", "Duyệt nhập kho", "/com/phungloccoffee/gui/view/inventory/ImportApproval.fxml"),
                    item("APPROVE_EXPORT", "Duyệt xuất kho", "↤", "Chi nhánh > Kho > Duyệt xuất kho", "Duyệt xuất kho", "/com/phungloccoffee/gui/view/inventory/ExportApproval.fxml"),
                    item("APPROVE_STOCKTAKE", "Duyệt kiểm kê kho", "▣", "Chi nhánh > Kho > Duyệt kiểm kê", "Duyệt kiểm kê kho", "/com/phungloccoffee/gui/view/inventory/StocktakeApproval.fxml"),
                    item("APPROVE_TRANSFER", "Duyệt điều chuyển kho", "⇄", "Chi nhánh > Kho > Duyệt điều chuyển", "Duyệt điều chuyển kho", "/com/phungloccoffee/gui/view/inventory/TransferApproval.fxml"),
                    item("APPROVE_MATERIAL_LOSS", "Duyệt hao hụt nguyên liệu", "!", "Chi nhánh > Kho > Duyệt hao hụt", "Duyệt hao hụt nguyên liệu", "/com/phungloccoffee/gui/view/inventory/MaterialLossApproval.fxml"),
                    item("BRANCH_EMPLOYEE", "Nhân viên chi nhánh", "◎", "Chi nhánh > Nhân viên", "Nhân viên chi nhánh", "/com/phungloccoffee/gui/view/employee/BranchEmployeeList.fxml"),
                    item("BRANCH_REVENUE_REPORT", "Báo cáo doanh thu chi nhánh", "▥", "Báo cáo > Doanh thu chi nhánh", "Báo cáo doanh thu chi nhánh", "/com/phungloccoffee/gui/view/report/BranchRevenueReport.fxml"),
                    item("BRANCH_INVENTORY_REPORT", "Báo cáo tồn kho chi nhánh", "▦", "Báo cáo > Tồn kho chi nhánh", "Báo cáo tồn kho chi nhánh", "/com/phungloccoffee/gui/view/report/BranchInventoryReport.fxml")
            );
            case BAN_GIAM_DOC -> List.of(
                    item("DIRECTOR_DASHBOARD", "Dashboard toàn hệ thống", "⌂", "Ban giám đốc > Dashboard", "Dashboard toàn hệ thống", "/com/phungloccoffee/gui/view/dashboard/DirectorDashboard.fxml"),
                    item("REVENUE_REPORT", "Báo cáo doanh thu", "▥", "Báo cáo > Doanh thu", "Báo cáo doanh thu", "/com/phungloccoffee/gui/view/RevenueReport.fxml"),
                    item("INVENTORY_REPORT", "Báo cáo tồn kho", "▦", "Báo cáo > Tồn kho", "Báo cáo tồn kho", "/com/phungloccoffee/gui/view/report/InventoryReport.fxml"),
                    item("BEST_SELLER_REPORT", "Mặt hàng bán chạy", "★", "Báo cáo > Mặt hàng bán chạy", "Mặt hàng bán chạy", "/com/phungloccoffee/gui/view/report/BestSellerReport.fxml"),
                    item("PRODUCT_MANAGEMENT", "Quản lý sản phẩm / menu", "▤", "Menu > Sản phẩm", "Quản lý sản phẩm / menu", "/com/phungloccoffee/gui/view/ProductManagement.fxml"),
                    item("BRANCH_MANAGEMENT", "Quản lý chi nhánh", "⌂", "Hệ thống > Chi nhánh", "Quản lý chi nhánh", "/com/phungloccoffee/gui/view/branch/BranchManagement.fxml")
            );
            case IT_ADMIN -> List.of(
                    item("ACCOUNT_LIST", "Quản lý tài khoản", "◎", "Hệ thống > Quản lý tài khoản", "Danh sách tài khoản", "/com/phungloccoffee/gui/view/EmployeeList.fxml"),
                    item("ACCOUNT_EDIT", "Chỉnh sửa tài khoản", "✎", "Hệ thống > Chỉnh sửa tài khoản", "Chỉnh sửa tài khoản", "/com/phungloccoffee/gui/view/AccountEdit.fxml"),
                    item("PERMISSION", "Phân quyền", "◆", "Hệ thống > Phân quyền", "Phân quyền người dùng", "/com/phungloccoffee/gui/view/account/Permission.fxml"),
                    item("BRANCH_MANAGEMENT", "Quản lý chi nhánh", "⌂", "Hệ thống > Chi nhánh", "Quản lý chi nhánh", "/com/phungloccoffee/gui/view/branch/BranchManagement.fxml"),
                    item("POS_DEVICE", "Quản lý thiết bị POS", "▣", "Hệ thống > Thiết bị POS", "Quản lý thiết bị POS", "/com/phungloccoffee/gui/view/system/PosDeviceManagement.fxml"),
                    item("SYSTEM_CONFIG", "Cấu hình hệ thống", "⚙", "Hệ thống > Cấu hình", "Cấu hình hệ thống", "/com/phungloccoffee/gui/view/system/SystemConfig.fxml")
            );
        };
    }

    public static MenuItemModel getDefaultMenuByRole(AppRole role) {
        return getMenuByRole(role).get(0);
    }

    public static Optional<MenuItemModel> findMenuItem(AppRole role, String menuId) {
        return getMenuByRole(role).stream()
                .filter(item -> item.getId().equals(menuId))
                .findFirst();
    }

    private static MenuItemModel item(String id, String title, String iconText, String breadcrumb,
                                      String pageTitle, String fxmlPath) {
        return new MenuItemModel(id, title, iconText, breadcrumb, pageTitle, fxmlPath, true);
    }
}
