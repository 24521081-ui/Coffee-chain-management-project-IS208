package com.phungloccoffee.bus;

import com.phungloccoffee.dao.InventoryDAO;
import com.phungloccoffee.dao.WarehouseTransactionDAO;
import com.phungloccoffee.dao.WarehouseWorkflowDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.WarehouseSlipStatus;
import com.phungloccoffee.model.WarehouseTransaction;
import com.phungloccoffee.model.WarehouseTransactionDetail;
import com.phungloccoffee.util.AutoCodeGenerator;
import com.phungloccoffee.util.SessionManager;
import com.phungloccoffee.util.ValidationUtils;

import java.time.LocalDateTime;
import java.util.List;

public class InventoryBUS extends PermissionBUS {
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final WarehouseWorkflowDAO workflowDAO = new WarehouseWorkflowDAO();
    private final WarehouseTransactionDAO transactionDAO = new WarehouseTransactionDAO();

    public List<InventoryItem> loadInventoryItems() throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return inventoryDAO.findAll();
    }

    public List<InventoryItem> loadInventoryItemsForCurrentBranch() throws DatabaseException, PermissionException, ValidationException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        String branchId = SessionManager.getCurrentBranchId();
        if (branchId == null || branchId.isBlank()) {
            return inventoryDAO.findAll();
        }
        String khoId = workflowDAO.findDefaultWarehouseByBranch(branchId)
                .orElseThrow(() -> new ValidationException("KhÃ´ng tÃ¬m tháº¥y kho cho chi nhÃ¡nh hiá»‡n táº¡i."));
        return inventoryDAO.findByKho(khoId);
    }

    public List<InventoryItem> loadBranchInventory(String branchId) throws DatabaseException, PermissionException, ValidationException {
        requireRole("THU_NGAN", "NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(branchId, "Chi nhanh");
        return inventoryDAO.findByBranch(branchId).getItems();
    }

    public void createTransaction(String type, String branchName, List<WarehouseTransactionDetail> details) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH");
        ValidationUtils.requireText(type, "Loáº¡i phiáº¿u");
        ValidationUtils.requireText(branchName, "Chi nhÃ¡nh");
        if (details == null || details.isEmpty()) {
            throw new ValidationException("Phiáº¿u kho chÆ°a cÃ³ nguyÃªn liá»‡u.");
        }
        WarehouseTransaction transaction = new WarehouseTransaction(
                0,
                nextTransactionCode(type),
                type,
                branchName,
                WarehouseSlipStatus.PENDING_APPROVAL,
                0,
                LocalDateTime.now()
        );
        transactionDAO.create(transaction, details);
    }

    private String nextTransactionCode(String type) throws DatabaseException {
        return AutoCodeGenerator.generateNextCode(prefixFor(type), transactionDAO.findTransactionCodes());
    }

    private String prefixFor(String type) {
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        if (normalizedType.contains("NHAP") || normalizedType.contains("IMPORT")) {
            return "PN";
        }
        if (normalizedType.contains("DIEU") || normalizedType.contains("TRANSFER") || normalizedType.contains("DC")) {
            return "DC";
        }
        return "PX";
    }
}
