package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Income;

public interface IIncomeService {
    Income save(Income income, boolean isRecurring);
}
