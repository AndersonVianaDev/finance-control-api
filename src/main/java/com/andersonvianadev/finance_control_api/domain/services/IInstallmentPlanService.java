package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;

public interface IInstallmentPlanService {
    record CreationResult(InstallmentPlan plan, Expense firstExpense) {}

    CreationResult create(InstallmentPlan plan, boolean skipBudget);

    void generateRemainingInstallments(java.util.UUID planId, boolean skipBudget);
}
