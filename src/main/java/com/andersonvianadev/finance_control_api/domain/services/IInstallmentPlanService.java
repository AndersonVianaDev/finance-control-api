package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CreationResultDTO;

public interface IInstallmentPlanService {
    CreationResultDTO create(InstallmentPlan plan, boolean skipBudget);

    void generateRemainingInstallments(java.util.UUID planId, boolean skipBudget);
}
