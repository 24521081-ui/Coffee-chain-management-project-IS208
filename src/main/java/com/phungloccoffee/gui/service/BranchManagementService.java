package com.phungloccoffee.gui.service;

import com.phungloccoffee.dao.BranchManagementDAO;
import com.phungloccoffee.dao.ChiNhanhDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.ChiNhanh;
import com.phungloccoffee.model.branch.BranchManagementModels.BranchData;

import java.time.LocalDate;
import java.util.List;

public class BranchManagementService {
    private final BranchManagementDAO branchManagementDAO = new BranchManagementDAO();
    private final ChiNhanhDAO chiNhanhDAO = new ChiNhanhDAO();

    public List<BranchData> loadBranches(LocalDate startDate, LocalDate endDate) throws DatabaseException {
        return branchManagementDAO.findBranches(startDate, endDate);
    }

    public void saveBranch(BranchData branch) throws DatabaseException {
        chiNhanhDAO.save(new ChiNhanh(
                branch.code(),
                branch.name(),
                branch.address(),
                branch.phone(),
                branch.status()
        ));
    }

    public void updateStatus(String branchCode, int status) throws DatabaseException {
        chiNhanhDAO.updateStatus(branchCode, status);
    }
}
