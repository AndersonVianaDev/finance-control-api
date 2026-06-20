package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.infra.exceptions.OperationNotAllowedException;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements IExpenseService {

    private final ExpenseRepository repository;
    private final ICategoryService categoryService;
    private final IBudgetService budgetService;
    private final ICalendarService calendarService;

    @Override
    public Expense save(Expense expense, boolean skipBudget) {
        User owner = expense.getOwner();

        Category category = expense.getCategory();
        UUID categoryId = category.getId();

        category = categoryService.findByIdAndOwnerOrOwnerIsNull(categoryId, owner);
        expense.setCategory(category);

        boolean isInstallment = expense.getInstallmentPlan() != null;

        if(isInstallment) {
            LocalDateTime transactionDate = expense.getTransactionDate();
            CalendarDTO calendarDTO = calendarService.getDate(transactionDate.toLocalDate());

            if(!calendarDTO.isWorkingDay()) {
                transactionDate = calendarDTO.nextWorkingDay().atStartOfDay();
                expense.setTransactionDate(transactionDate);
            }
        }

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

    @Override
    public Expense findById(User user, UUID id) {
        return repository.findExpenseByOwnerIdAndId(user.getId(), id)
                .orElseThrow(() -> new NotFoundException(String.format("Expense with id %s not found", id.toString())));
    }

    @Override
    public Page<Expense> findAll(User user, Pageable pageable) {
        return repository.findByOwnerId(user.getId(), pageable);
    }

    @Override
    public void delete(User owner, UUID id) {
        Expense expense = this.findById(owner, id);

        if(expense.isInstallments()) {
            throw new OperationNotAllowedException("The user can only cancel the entire installment plan");
        }

        repository.delete(expense);
    }

    @Override
    @Transactional
    public Expense update(Expense expense, boolean skipBudget) {
        User owner = expense.getOwner();

        Expense expenseSaved = this.findById(owner, expense.getId());

        if(expenseSaved.isInstallments()) {
            throw new OperationNotAllowedException("The user can only update the installment plan in full.");
        }

        BigDecimal oldPrice = expenseSaved.getPrice();

        Category incomingCategory = expense.getCategory();
        Category resolvedCategory = expenseSaved.getCategory();
        boolean categoryChanged = false;

        if(incomingCategory != null && incomingCategory.getId() != null) {
            resolvedCategory = categoryService.findByIdAndOwnerOrOwnerIsNull(incomingCategory.getId(), owner);
            categoryChanged = true;
        }

        BigDecimal projectedPrice = (expense.getPrice() != null && !expense.getPrice().equals(oldPrice))
                ? expense.getPrice() : oldPrice;
        String projectedDescription = (expense.getDescription() != null && !expense.getDescription().equals(expenseSaved.getDescription()))
                ? expense.getDescription() : expenseSaved.getDescription();
        LocalDateTime projectedDate = (expense.getTransactionDate() != null && !expense.getTransactionDate().equals(expenseSaved.getTransactionDate()))
                ? expense.getTransactionDate() : expenseSaved.getTransactionDate();

        boolean exists = repository.existsDuplicateExcluding(
                owner.getId(),
                resolvedCategory.getId(),
                projectedPrice,
                projectedDate,
                projectedDescription,
                expenseSaved.getId()
        );

        if(exists) {
            throw new ResourceAlreadyExistsException("Expense already registered");
        }

        boolean priceChanged = !projectedPrice.equals(oldPrice);

        if(!skipBudget) {
            if(categoryChanged) {
                budgetService.validateTransactionRespectsBudget(owner, resolvedCategory, projectedPrice);
            } else if(priceChanged) {
                budgetService.validateTransactionRespectsBudgetOnUpdate(owner, expenseSaved.getCategory(), oldPrice, projectedPrice);
            }
        }

        if(categoryChanged) expenseSaved.setCategory(resolvedCategory);
        if(priceChanged) expenseSaved.setPrice(projectedPrice);
        if(!projectedDescription.equals(expenseSaved.getDescription())) expenseSaved.setDescription(projectedDescription);
        if(!projectedDate.equals(expenseSaved.getTransactionDate())) expenseSaved.setTransactionDate(projectedDate);

        try {
            return repository.save(expenseSaved);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while updating expense. owner={}, category={}, date={}, price={}",
                    owner.getEmail(),
                    expenseSaved.getCategory().getName(),
                    expenseSaved.getTransactionDate(),
                    expenseSaved.getPrice(),
                    e
            );
            throw new ResourceAlreadyExistsException("Expense already registered");
        }
    }

    @Override
    @Transactional
    public void deleteByInstallmentPlan(UUID planId) {
        repository.deleteByInstallmentPlanId(planId);
    }

    @Override
    public List<Expense> findByInstallmentPlan(UUID planId) {
        return repository.findByInstallmentPlanIdOrderByInstallmentNumberAsc(planId);
    }

    @Override
    public Page<Expense> findBetweenTransactionDate(User user, LocalDateTime start, LocalDateTime finish, Pageable pageable) {
        return repository.findByOwnerIdAndTransactionDateBetween(
                user.getId(), start,
                finish, pageable
        );
    }
}
