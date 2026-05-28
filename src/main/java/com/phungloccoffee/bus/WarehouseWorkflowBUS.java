package com.phungloccoffee.bus;

import com.phungloccoffee.dao.WarehouseWorkflowDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.NhaCungCap;
import com.phungloccoffee.model.WarehouseApprovalItem;
import com.phungloccoffee.model.WarehouseSlip;
import com.phungloccoffee.model.WarehouseSlipLine;
import com.phungloccoffee.model.WarehouseSlipStatus;
import com.phungloccoffee.model.WarehouseSlipType;
import com.phungloccoffee.util.SessionManager;
import com.phungloccoffee.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class WarehouseWorkflowBUS extends PermissionBUS {
    private static final BigDecimal STOCKTAKE_EXPLANATION_THRESHOLD_PERCENT = new BigDecimal("2");

    private final WarehouseWorkflowDAO workflowDAO = new WarehouseWorkflowDAO();

    public List<NhaCungCap> loadActiveSuppliers() throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return workflowDAO.findActiveSuppliers();
    }

    public List<InventoryItem> loadMaterialsForCurrentBranch() throws DatabaseException, PermissionException, ValidationException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        String khoId = resolveCurrentWarehouseId();
        return workflowDAO.findMaterialsByWarehouse(khoId);
    }

    public String resolveCurrentWarehouseId() throws DatabaseException, ValidationException {
        return workflowDAO.findDefaultWarehouseByBranch(SessionManager.getCurrentBranchId())
                .orElseThrow(() -> new ValidationException("Không tìm thấy kho hoạt động cho chi nhánh hiện tại."));
    }

    public void saveImportDraft(WarehouseSlip slip) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        prepareAndValidateImport(slip, WarehouseSlipStatus.DRAFT);
        workflowDAO.createSlip(slip);
    }

    public void submitImport(WarehouseSlip slip) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        prepareAndValidateImport(slip, WarehouseSlipStatus.PENDING_APPROVAL);
        workflowDAO.createSlip(slip);
    }

    public void saveExportDraft(WarehouseSlip slip) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        prepareAndValidateExport(slip, WarehouseSlipStatus.DRAFT);
        workflowDAO.createSlip(slip);
    }

    public void submitExport(WarehouseSlip slip) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        prepareAndValidateExport(slip, WarehouseSlipStatus.PENDING_APPROVAL);
        workflowDAO.createSlip(slip);
    }

    public void saveStocktakeDraft(WarehouseSlip slip) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        prepareAndValidateStocktake(slip, WarehouseSlipStatus.DRAFT);
        workflowDAO.createSlip(slip);
    }

    public void submitStocktake(WarehouseSlip slip) throws ValidationException, PermissionException, DatabaseException {
        requireRole("NHAN_VIEN_KHO");
        prepareAndValidateStocktake(slip, WarehouseSlipStatus.PENDING_APPROVAL);
        workflowDAO.createSlip(slip);
    }

    public List<WarehouseApprovalItem> loadPendingApprovalItems(String type)
            throws DatabaseException, PermissionException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return workflowDAO.findPendingApprovalItems(SessionManager.getCurrentBranchId(), type);
    }

    public List<WarehouseApprovalItem> loadApprovalHistory(String type)
            throws DatabaseException, PermissionException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return workflowDAO.findApprovalHistory(SessionManager.getCurrentBranchId(), type);
    }

    public List<WarehouseApprovalItem> loadMySlipHistory(String type)
            throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "IT_ADMIN");
        return workflowDAO.findEmployeeSlipHistory(
                SessionManager.getCurrentEmployeeId(),
                SessionManager.getCurrentBranchId(),
                type
        );
    }

    public List<WarehouseSlipLine> loadStocktakeDetails(String slipId)
            throws DatabaseException, PermissionException, ValidationException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(slipId, "Mã phiếu");
        return workflowDAO.findStocktakeDetails(slipId);
    }

    public void approve(String type, String slipId) throws ValidationException, PermissionException, DatabaseException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(type, "Loại phiếu");
        ValidationUtils.requireText(slipId, "Mã phiếu");
        workflowDAO.approveSlip(type, slipId, SessionManager.getCurrentEmployeeId());
    }

    public void reject(String type, String slipId, String rejectedReason)
            throws ValidationException, PermissionException, DatabaseException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(type, "Loại phiếu");
        ValidationUtils.requireText(slipId, "Mã phiếu");
        ValidationUtils.requireText(rejectedReason, "Lý do từ chối");
        workflowDAO.rejectSlip(type, slipId, SessionManager.getCurrentEmployeeId(), rejectedReason.trim());
    }

    private void prepareAndValidateImport(WarehouseSlip slip, String status) throws ValidationException, DatabaseException {
        prepareCommonSlip(slip, WarehouseSlipType.IMPORT, status);
        ValidationUtils.requireText(slip.getSupplierId(), "Nhà cung cấp");
        for (WarehouseSlipLine line : slip.getLines()) {
            validateLineIdentity(line);
            validatePositive(line.getQuantity(), "Số lượng nhập");
        }
    }

    private void prepareAndValidateExport(WarehouseSlip slip, String status) throws ValidationException, DatabaseException {
        prepareCommonSlip(slip, WarehouseSlipType.EXPORT, status);
        ValidationUtils.requireText(slip.getReason(), "Lý do xuất/hủy");
        List<InventoryItem> materials = workflowDAO.findMaterialsByWarehouse(slip.getKhoId());
        for (WarehouseSlipLine line : slip.getLines()) {
            validateLineIdentity(line);
            validatePositive(line.getQuantity(), "Số lượng xuất");
            InventoryItem item = materials.stream()
                    .filter(material -> material.getItemCode().equals(line.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Nguyên liệu " + line.getItemId() + " không hợp lệ."));
            if (item.getQuantityOnHand().compareTo(line.getQuantity()) < 0) {
                throw new ValidationException("Số lượng xuất của " + line.getItemId() + " vượt tồn kho khả dụng.");
            }
        }
    }

    private void prepareAndValidateStocktake(WarehouseSlip slip, String status) throws ValidationException, DatabaseException {
        prepareCommonSlip(slip, WarehouseSlipType.STOCKTAKE, status);
        List<InventoryItem> materials = workflowDAO.findMaterialsByWarehouse(slip.getKhoId());
        for (WarehouseSlipLine line : slip.getLines()) {
            validateLineIdentity(line);
            if (line.getActualQuantity() == null || line.getActualQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Số lượng thực tế phải lớn hơn hoặc bằng 0.");
            }
            BigDecimal systemQty = materials.stream()
                    .filter(material -> material.getItemCode().equals(line.getItemId()))
                    .findFirst()
                    .map(InventoryItem::getQuantityOnHand)
                    .orElseThrow(() -> new ValidationException("Nguyên liệu " + line.getItemId() + " không hợp lệ."));
            line.setSystemQuantity(systemQty);
            if (requiresStocktakeExplanation(systemQty, line.getActualQuantity())
                    && (line.getNote() == null || line.getNote().isBlank())) {
                throw new ValidationException("Cần nhập giải trình cho các dòng kiểm kê có chênh lệch vượt 2%.");
            }
        }
    }

    private void prepareCommonSlip(WarehouseSlip slip, String type, String status) throws ValidationException, DatabaseException {
        if (slip == null) {
            throw new ValidationException("Phiếu kho không hợp lệ.");
        }
        String khoId = slip.getKhoId();
        if (khoId == null || khoId.isBlank()) {
            khoId = resolveCurrentWarehouseId();
            slip.setKhoId(khoId);
        }
        slip.setSlipType(type);
        slip.setStatus(status);
        slip.setCreatedBy(SessionManager.getCurrentEmployeeId());
        if (slip.getSlipId() == null || slip.getSlipId().isBlank()) {
            slip.setSlipId(generateSlipId(type));
        }
        if (slip.getLines() == null || slip.getLines().isEmpty()) {
            throw new ValidationException("Phiếu kho phải có ít nhất một dòng chi tiết.");
        }
    }

    private void validateLineIdentity(WarehouseSlipLine line) throws ValidationException {
        if (line == null) {
            throw new ValidationException("Dòng chi tiết không hợp lệ.");
        }
        ValidationUtils.requireText(line.getItemId(), "Nguyên liệu");
    }

    private void validatePositive(BigDecimal value, String fieldName) throws ValidationException {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " phải lớn hơn 0.");
        }
    }

    private boolean requiresStocktakeExplanation(BigDecimal systemQty, BigDecimal actualQty) {
        BigDecimal safeSystemQty = zeroIfNull(systemQty);
        BigDecimal safeActualQty = zeroIfNull(actualQty);
        BigDecimal delta = safeActualQty.subtract(safeSystemQty).abs();
        if (safeSystemQty.compareTo(BigDecimal.ZERO) <= 0) {
            return delta.compareTo(BigDecimal.ZERO) > 0;
        }
        BigDecimal percent = delta.multiply(BigDecimal.valueOf(100))
                .divide(safeSystemQty, 4, RoundingMode.HALF_UP);
        return percent.compareTo(STOCKTAKE_EXPLANATION_THRESHOLD_PERCENT) > 0;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateSlipId(String type) {
        String prefix = switch (type) {
            case WarehouseSlipType.IMPORT -> "PNK";
            case WarehouseSlipType.EXPORT -> "PXK";
            case WarehouseSlipType.STOCKTAKE -> "KKK";
            default -> "WH";
        };
        return prefix + "-" + System.currentTimeMillis();
    }
}
