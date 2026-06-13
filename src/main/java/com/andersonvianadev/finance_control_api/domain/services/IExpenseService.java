package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Expense;

public interface IExpenseService {
    Expense save(Expense expense, boolean skipBudget);
}
