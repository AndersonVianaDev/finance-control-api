package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements IExpenseService {

    private final ExpenseRepository repository;
    private final ICategoryService categoryService;
    private final IBudgetService budgetService;

    @Override
    public Expense save(Expense expense, boolean skipBudget) {
        User owner = expense.getOwner();

        Category category = expense.getCategory();
        UUID categoryId = category.getId();

        category = categoryService.findByIdAndOwnerOrOwnerIsNull(categoryId, owner);
        expense.setCategory(category);

        boolean isInstallment = expense.getInstallmentPlan() != null;

        boolean exists = !isInstallment && repository.existsDuplicate(
                owner.getId(),
                category.getId(),
                expense.getPrice(),
                expense.getTransactionDate(),
                expense.getDescription()
        );

        if (exists) {
            log.warn(
                    "Attempt to create duplicate expense. owner={}, category={}, date={}, price={}",
                    owner.getEmail(),
                    category.getName(),
                    expense.getTransactionDate(),
                    expense.getPrice()
            );
            throw new ResourceAlreadyExistsException("Expense already registered");
        }

        if (!skipBudget) {
            budgetService.validateTransactionRespectsBudget(owner, category, expense.getPrice());
        }

        try {
            return repository.save(expense);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while creating expense. owner={}, category={}, date={}, price={}",
                    owner.getEmail(),
                    category.getName(),
                    expense.getTransactionDate(),
                    expense.getPrice(),
                    e
            );
            throw new ResourceAlreadyExistsException("Expense already registered");
        }
    }
}
