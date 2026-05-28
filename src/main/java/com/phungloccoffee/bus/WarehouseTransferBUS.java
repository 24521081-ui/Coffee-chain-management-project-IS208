package com.phungloccoffee.bus;

import com.phungloccoffee.dao.WarehouseTransferDAO;
import com.phungloccoffee.dao.WarehouseWorkflowDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.Kho;
import com.phungloccoffee.model.WarehouseTransferLine;
import com.phungloccoffee.model.WarehouseTransferSlip;
import com.phungloccoffee.util.SessionManager;
import com.phungloccoffee.util.ValidationUtils;

import java.math.BigDecimal;
import java.util.List;

public class WarehouseTransferBUS extends PermissionBUS {
    private final WarehouseTransferDAO transferDAO = new WarehouseTransferDAO();
    private final WarehouseWorkflowDAO workflowDAO = new WarehouseWorkflowDAO();

    public Kho resolveCurrentWarehouse() throws DatabaseException, ValidationException {
        String khoId = workflowDAO.findDefaultWarehouseByBranch(SessionManager.getCurrentBranchId())
                .orElseThrow(() -> new ValidationException("Không tìm thấy kho hoạt động cho chi nhánh hiện tại."));
        return transferDAO.findWarehouseById(khoId)
                .orElseThrow(() -> new ValidationException("Không tìm thấy thông tin kho nguồn hiện tại."));
    }

    public List<Kho> loadTargetWarehouses(String sourceWarehouseId) throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return transferDAO.findActiveWarehouses().stream()
                .filter(item -> !item.getKhoId().equals(sourceWarehouseId))
                .toList();
    }

    public List<InventoryItem> loadSourceMaterials() throws DatabaseException, PermissionException, ValidationException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return workflowDAO.findMaterialsByWarehouse(resolveCurrentWarehouse().getKhoId());
    }

    public void submitTransfer(WarehouseTransferSlip slip) throws DatabaseException, PermissionException, ValidationException {
        requireRole("NHAN_VIEN_KHO");
        if (slip == null) {
            throw new ValidationException("Phiếu điều chuyển không hợp lệ.");
        }
        ValidationUtils.requireText(slip.getSourceWarehouseId(), "Kho nguồn");
        ValidationUtils.requireText(slip.getTargetWarehouseId(), "Kho đích");
        if (slip.getSourceWarehouseId().equals(slip.getTargetWarehouseId())) {
            throw new ValidationException("Kho nguồn và kho đích không được trùng nhau.");
        }
        if (slip.getLines() == null || slip.getLines().isEmpty()) {
            throw new ValidationException("Phiếu điều chuyển phải có ít nhất một nguyên liệu.");
        }

        List<InventoryItem> sourceMaterials = workflowDAO.findMaterialsByWarehouse(slip.getSourceWarehouseId());
        for (WarehouseTransferLine line : slip.getLines()) {
            ValidationUtils.requireText(line.getItemId(), "Nguyên liệu");
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Số lượng điều chuyển phải lớn hơn 0.");
            }
            InventoryItem item = sourceMaterials.stream()
                    .filter(material -> material.getItemCode().equals(line.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Nguyên liệu " + line.getItemId() + " không tồn tại trong kho nguồn."));
            if (item.getQuantityOnHand().compareTo(line.getQuantity()) < 0) {
                throw new ValidationException("Số lượng điều chuyển của " + line.getItemId() + " vượt tồn kho khả dụng.");
            }
        }

        if (slip.getSlipId() == null || slip.getSlipId().isBlank()) {
            slip.setSlipId("PDC" + System.currentTimeMillis());
        }
        slip.setCreatedBy(SessionManager.getCurrentEmployeeId());
        transferDAO.createTransfer(slip);
    }

    public List<WarehouseTransferSlip> loadPendingTransfers() throws DatabaseException, PermissionException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return transferDAO.findTransfersForApproval(SessionManager.getCurrentBranchId());
    }

    public void approveTransfer(String slipId) throws DatabaseException, PermissionException, ValidationException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(slipId, "Mã phiếu");
        transferDAO.approveTransfer(slipId);
    }

    public void rejectTransfer(String slipId) throws DatabaseException, PermissionException, ValidationException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(slipId, "Mã phiếu");
        transferDAO.rejectTransfer(slipId);
    }
}
