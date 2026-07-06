package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IExpenseService {
    Expense save(Expense expense, boolean isRecurring, boolean skipBudget);
    Expense findById(User user, UUID id);
    Page<Expense> findAll(User user, Pageable pageable);
    void delete(User user, UUID id);
    Expense update(Expense expense, boolean skipBudget);
    void deleteByInstallmentPlan(UUID planId);
    List<Expense> findByInstallmentPlan(UUID planId);
    Page<Expense> findBetweenTransactionDate(User user, LocalDateTime start, LocalDateTime finish, Pageable pageable);
    BigDecimal sumByOwnerAndDateRange(User user, LocalDate start, LocalDate finish);
    Integer countByOwnerAndDateRange(User user, LocalDate start, LocalDate finish);
    List<Expense> findProjectedExpensesByDateRange(User user, LocalDate start, LocalDate finish);
    List<Expense> findScheduledExpensesByDateRange(User user, LocalDate start, LocalDate finish);
}
