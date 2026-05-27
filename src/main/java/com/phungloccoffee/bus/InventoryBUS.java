package com.phungloccoffee.bus;

import com.phungloccoffee.dao.InventoryDAO;
import com.phungloccoffee.dao.WarehouseTransactionDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.WarehouseTransaction;
import com.phungloccoffee.model.WarehouseTransactionDetail;
import com.phungloccoffee.util.ValidationUtils;

import java.time.LocalDateTime;
import java.util.List;

public class InventoryBUS extends PermissionBUS {
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final WarehouseTransactionDAO transactionDAO = new WarehouseTransactionDAO();

    public List<InventoryItem> loadInventoryItems() throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return inventoryDAO.findAll();
    }

    public List<InventoryItem> loadBranchInventory(String branchId) throws DatabaseException, PermissionException, ValidationException {
        requireRole("THU_NGAN", "NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(branchId, "Chi nhanh");
        return inventoryDAO.findByBranch(branchId).getItems();
    }

    public void createTransaction(String type, String branchName, List<WarehouseTransactionDetail> details) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH");
        ValidationUtils.requireText(type, "Loại phiếu");
        ValidationUtils.requireText(branchName, "Chi nhánh");
        if (details == null || details.isEmpty()) {
            throw new ValidationException("Phiếu kho chưa có nguyên liệu.");
        }
        WarehouseTransaction transaction = new WarehouseTransaction(0, "WH" + System.currentTimeMillis(), type, branchName, "NHAP", 0, LocalDateTime.now());
        transactionDAO.create(transaction, details);
    }
}
