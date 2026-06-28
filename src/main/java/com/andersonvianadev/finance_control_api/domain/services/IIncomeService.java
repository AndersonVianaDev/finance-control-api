package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IIncomeService {
    Income save(Income income, boolean isRecurring);
    Income findById(User user, UUID id);
    Page<Income> findAll(User user, Pageable pageable);
    void delete(User user, UUID id);
    Income update(Income income);
    Page<Income> findBetweenTransactionDate(User user, LocalDateTime start, LocalDateTime finish, Pageable pageable);
}
