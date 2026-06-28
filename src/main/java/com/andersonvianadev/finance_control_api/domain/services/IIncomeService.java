package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public interface IIncomeService {
    Income save(Income income, boolean isRecurring);
    Income findById(User user, UUID id);
}
