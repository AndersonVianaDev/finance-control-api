package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
}
