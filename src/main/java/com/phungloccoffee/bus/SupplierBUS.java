package com.phungloccoffee.bus;

import com.phungloccoffee.dao.SupplierDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.model.SupplierDirectoryItem;

import java.util.List;

public class SupplierBUS extends PermissionBUS {
    private final SupplierDAO supplierDAO = new SupplierDAO();

    public List<SupplierDirectoryItem> loadSuppliers() throws DatabaseException, PermissionException {
        requireRole("NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return supplierDAO.findAllSuppliers();
    }
}
