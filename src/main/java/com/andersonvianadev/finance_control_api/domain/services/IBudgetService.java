package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public interface IBudgetService {
    Budget save(Budget budget);
    Budget findById(User owner, UUID id);
    Budget update(Budget budget);
    void delete(User owner, UUID id);
}
