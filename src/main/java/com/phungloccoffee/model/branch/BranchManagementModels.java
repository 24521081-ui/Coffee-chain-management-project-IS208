package com.phungloccoffee.model.branch;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BranchManagementModels {
    private BranchManagementModels() {
    }

    public record BranchData(String code,
                             String name,
                             String area,
                             String address,
                             String phone,
                             String manager,
                             String managerPhone,
                             String managerEmail,
                             LocalDate openingDate,
                             int status,
                             LocalDate statusChangedDate,
                             String note,
                             int employeeCount,
                             int servingProductCount,
                             int orderCount,
                             BigDecimal revenue,
                             int ingredientCount) {
    }
}
