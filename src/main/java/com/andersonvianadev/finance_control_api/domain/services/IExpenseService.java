package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IExpenseService {
    Expense save(Expense expense, boolean skipBudget);
    Expense findById(User user, UUID id);
    Page<Expense> findAll(User user, Pageable pageable);
}
