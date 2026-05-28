package com.phungloccoffee.bus;

import com.phungloccoffee.dao.MaterialLossDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.MaterialLossRecord;
import com.phungloccoffee.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MaterialLossBUS extends PermissionBUS {
    private static final String REASON_OTHER = "Khác";

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
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
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        materialLossDAO.insert(record);
    }

    private String generateLossId() {
        return "HH" + System.currentTimeMillis();
    }
}
