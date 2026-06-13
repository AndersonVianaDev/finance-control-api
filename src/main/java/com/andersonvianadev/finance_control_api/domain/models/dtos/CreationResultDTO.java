package com.andersonvianadev.finance_control_api.domain.models.dtos;

import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;

public record CreationResultDTO(InstallmentPlan plan, Expense firstExpense) {
}
