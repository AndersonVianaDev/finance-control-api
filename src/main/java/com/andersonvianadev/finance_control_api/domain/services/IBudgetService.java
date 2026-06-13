package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface IBudgetService {
    Budget save(Budget budget);
    Budget findById(User owner, UUID id);
    Budget update(Budget budget);
    void delete(User owner, UUID id);
    Page<Budget> findAll(User owner, Pageable pageable);
    void validateTransactionRespectsBudget(User owner, Category category, BigDecimal valueTransaction);
}
