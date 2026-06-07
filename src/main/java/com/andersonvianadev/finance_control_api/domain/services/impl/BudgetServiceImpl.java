package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements IBudgetService {

    private final BudgetRepository repository;
    private final ICategoryService categoryService;

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
}
