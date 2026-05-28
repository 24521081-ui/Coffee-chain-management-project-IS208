package com.phungloccoffee.bus;

import com.phungloccoffee.dao.MaterialLossDAO;
import com.phungloccoffee.dao.WarehouseWorkflowDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.MaterialLossRecord;
import com.phungloccoffee.util.SessionManager;
import com.phungloccoffee.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class MaterialLossBUS extends PermissionBUS {
    private static final String REASON_OTHER = "Khác";

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final WarehouseWorkflowDAO workflowDAO = new WarehouseWorkflowDAO();
    private final MaterialLossDAO materialLossDAO = new MaterialLossDAO();

    public List<InventoryItem> loadMaterialsForCurrentBranch()
            throws DatabaseException, PermissionException, ValidationException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return workflowBUS.loadMaterialsForCurrentBranch();
    }

    public List<MaterialLossRecord> loadLossHistory() throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return materialLossDAO.findAllLossRecords();
    }

    public List<MaterialLossRecord> loadLossHistoryForCurrentBranch()
            throws DatabaseException, PermissionException, ValidationException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        String branchId = SessionManager.getCurrentBranchId();
        ValidationUtils.requireText(branchId, "Chi nhánh");
        String warehouseId = workflowDAO.findDefaultWarehouseByBranch(branchId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy kho mặc định của chi nhánh hiện tại."));
        return materialLossDAO.findAllLossRecords().stream()
                .filter(record -> record.getWarehouseId() == null || warehouseId.equals(record.getWarehouseId()))
                .toList();
    }

    public void approveLoss(String lossId) throws ValidationException, PermissionException, DatabaseException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(lossId, "Mã hao hụt");
        materialLossDAO.approveLossRecord(lossId.trim());
    }

    public void rejectLoss(String lossId, String rejectedReason)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(lossId, "Mã hao hụt");
        ValidationUtils.requireText(rejectedReason, "Lý do từ chối");
        materialLossDAO.rejectLossRecord(lossId.trim(), rejectedReason.trim());
    }

    public void recordLoss(MaterialLossRecord record)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        if (record == null) {
            throw new ValidationException("Bản ghi hao hụt không hợp lệ.");
        }
        ValidationUtils.requireText(record.getMaterialId(), "Nguyên liệu");
        if (record.getQuantity() == null || record.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Số lượng hao hụt phải lớn hơn 0.");
        }
        ValidationUtils.requireText(record.getReason(), "Lý do hao hụt");
        if (REASON_OTHER.equals(record.getReason())) {
            ValidationUtils.requireText(record.getNote(), "Ghi chú");
        }

        List<InventoryItem> materials = workflowBUS.loadMaterialsForCurrentBranch();
        boolean exists = materials.stream().anyMatch(item -> item.getItemCode().equals(record.getMaterialId()));
        if (!exists) {
            throw new ValidationException("Nguyên liệu được chọn không tồn tại trong kho chi nhánh hiện tại.");
        }

        if (record.getLossId() == null || record.getLossId().isBlank()) {
            record.setLossId(generateLossId());
        }
        if (record.getWarehouseId() == null || record.getWarehouseId().isBlank()) {
            String branchId = SessionManager.getCurrentBranchId();
            ValidationUtils.requireText(branchId, "Chi nhánh");
            record.setWarehouseId(workflowDAO.findDefaultWarehouseByBranch(branchId)
                    .orElseThrow(() -> new ValidationException("Không tìm thấy kho mặc định của chi nhánh hiện tại.")));
        }
        if (record.getEmployeeId() == null || record.getEmployeeId().isBlank()) {
            ValidationUtils.requireText(SessionManager.getCurrentEmployeeId(), "Nhân viên");
            record.setEmployeeId(SessionManager.getCurrentEmployeeId());
        }
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        materialLossDAO.insert(record);
    }

    private String generateLossId() {
        String encodedTime = Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        return "HH" + encodedTime.substring(Math.max(0, encodedTime.length() - 8));
    }
}
