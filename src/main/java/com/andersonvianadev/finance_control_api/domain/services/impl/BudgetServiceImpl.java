package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.PeriodDTO;
import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.infra.exceptions.BudgetExceededException;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements IBudgetService {

    private final BudgetRepository repository;
    private final ICategoryService categoryService;
    private final ExpenseRepository expenseRepository;

    @Override
    public Budget save(Budget budget) {
        UUID categoryId = budget.getCategory().getId();
        User owner = budget.getOwner();

        Category category = categoryService.findByIdAndOwnerOrOwnerIsNull(categoryId, owner);

        if (repository.existsByCategoryAndOwner(category, owner)) {
            log.warn(
                    "Attempt to create budget with existing category: {}",
                    category.getName()
            );
            throw new ResourceAlreadyExistsException(
                    String.format("A budget for category %s already exists.", category.getName())
            );
        }

        budget.setCategory(category);

        try {
            return repository.save(budget);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while creating budget. category={}, owner={}",
                    category.getName(),
                    owner.getEmail(),
                    e
            );
            throw new ResourceAlreadyExistsException(
                    String.format("A budget for category %s already exists.", category.getName())
            );
        }
    }

    @Override
    public Budget findById(User owner, UUID id) {
        return repository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NotFoundException(String.format("Budget with id %s not found", id.toString())));
    }

    @Override
    public Budget update(Budget budget) {
        Budget budgetSaved = this.findById(budget.getOwner(), budget.getId());

        if(budget.getLimitAmount() != null && !budget.getLimitAmount().equals(budgetSaved.getLimitAmount())) {
            budgetSaved.setLimitAmount(budget.getLimitAmount());
        }

        if(budget.getBudgetType() != null && !budget.getBudgetType().equals(budgetSaved.getBudgetType())) {
            budgetSaved.setBudgetType(budget.getBudgetType());
        }

        if(budget.getActive() != null && !budget.getActive().equals(budgetSaved.getActive())) {
            budgetSaved.setActive(budget.getActive());
        }

        return repository.save(budgetSaved);
    }

    @Override
    public void delete(User owner, UUID id) {
        Budget budget = this.findById(owner, id);
        repository.delete(budget);
    }

    @Override
    public Page<Budget> findAll(User owner, Pageable pageable) {
        return repository.findByOwner(owner, pageable);
    }

    @Override
    public void validateTransactionRespectsBudget(User owner, Category category, BigDecimal valueTransaction) {
        UUID ownerId = owner.getId();
        UUID categoryId = category.getId();

        Optional<Budget> budgetOptional = repository.findByOwnerIdAndCategoryIdAndActiveTrue(ownerId, categoryId);

        if(budgetOptional.isPresent()) {
            Budget budget = budgetOptional.get();
            BudgetType budgetType = budget.getBudgetType();

            PeriodDTO period = budgetType.getPeriod();
            BigDecimal totalSpent = expenseRepository.sumByOwnerAndCategoryAndDateRange(
                    ownerId, categoryId, period.startDate(), period.endDate());

            if (totalSpent.add(valueTransaction).compareTo(budget.getLimitAmount()) > 0) {
                throw new BudgetExceededException("Budget limit exceeded for category: " + category.getName());
            }
        }

    }
}
