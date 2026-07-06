package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.models.enums.TransactionPeriodType;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import com.andersonvianadev.finance_control_api.domain.services.IRecurringRuleService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.RecurringRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringRuleServiceImpl implements IRecurringRuleService {

    private final RecurringRuleRepository repository;
    private final ICategoryService categoryService;
    private final IExpenseService expenseService;
    private final IIncomeService incomeService;

    @Override
    public RecurringRule save(RecurringRule recurringRule) {
        User owner = recurringRule.getOwner();

        Category category = recurringRule.getCategory();
        UUID categoryId = category.getId();
        category = categoryService.findByIdAndOwnerOrOwnerIsNull(categoryId, owner);

        recurringRule.setCategory(category);

        boolean exists = repository.existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescription(
                owner.getId(),
                categoryId,
                recurringRule.getPrice(),
                recurringRule.getTransactionDate(),
                recurringRule.getDescription()
        );

        if (exists) {
            throw new ResourceAlreadyExistsException("Recurring rule already registered");
        }

        return repository.save(recurringRule);
    }

    @Override
    public void processRecurringOccurrences() {
        LocalDate today = LocalDate.now();
        List<RecurringRule> activeRules = repository.findAllByIsActiveTrue();

        log.info("Processing recurring occurrences for {}. Active rules: {}", today, activeRules.size());

        for (RecurringRule rule : activeRules) {
            if (!isDueToday(rule, today)) {
                continue;
            }

            try {
                generateTransaction(rule, today);
                log.info("Generated recurring transaction. rule={}, owner={}, type={}",
                        rule.getId(), rule.getOwner().getEmail(), rule.getRecurringType());
            } catch (Exception e) {
                log.error("Failed to generate recurring transaction for rule={}. Skipping.", rule.getId(), e);
            }
        }
    }

    @Override
    public RecurringRule findById(User user, UUID id) {
        return repository.findByOwnerIdAndId(user.getId(), id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Recurring rule with id %s not found", id)
                ));
    }

    @Override
    public Page<RecurringRule> findAll(User user, Pageable pageable) {
        return repository.findAllByOwnerId(user.getId(), pageable);
    }

    @Override
    public void delete(User user, UUID id) {
        RecurringRule rule = findById(user, id);
        repository.delete(rule);
    }

    @Override
    public RecurringRule toggle(User user, UUID id) {
        RecurringRule rule = findById(user, id);
        rule.setIsActive(!rule.getIsActive());
        return repository.save(rule);
    }

    @Override
    public RecurringRule update(RecurringRule rule) {
        RecurringRule existing = findById(rule.getOwner(), rule.getId());

        UUID categoryId = rule.getCategory().getId();
        Category category = categoryService.findByIdAndOwnerOrOwnerIsNull(categoryId, rule.getOwner());

        boolean exists = repository.existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescriptionAndIdNot(
                rule.getOwner().getId(),
                categoryId,
                rule.getPrice(),
                rule.getTransactionDate(),
                rule.getDescription(),
                existing.getId()
        );

        if (exists) {
            throw new ResourceAlreadyExistsException("Recurring rule already registered");
        }

        existing.setTransactionDate(rule.getTransactionDate());
        existing.setPrice(rule.getPrice());
        existing.setDescription(rule.getDescription());
        existing.setCategory(category);
        existing.setTransactionPeriodType(rule.getTransactionPeriodType());
        existing.setRecurringType(rule.getRecurringType());

        return repository.save(existing);
    }

    @Override
    public List<RecurringRule> findByRangeDateAndType(User user, LocalDate start, LocalDate finish, RecurringType type) {
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime finishTime = finish.atTime(23, 59, 59);

        return repository.findByOwnerIdAndRecurringTypeAndTransactionDateBetween(
                user.getId(),
                type,
                startTime,
                finishTime
        );
    }

    private boolean isDueToday(RecurringRule rule, LocalDate today) {
        LocalDateTime ruleDate = rule.getTransactionDate();
        if (rule.getTransactionPeriodType() == TransactionPeriodType.MONTHLY) {
            return today.getDayOfMonth() == ruleDate.getDayOfMonth();
        }
        return today.getDayOfWeek() == ruleDate.getDayOfWeek();
    }

    private void generateTransaction(RecurringRule rule, LocalDate today) {
        LocalDateTime transactionDate = today.atStartOfDay();

        if (rule.getRecurringType() == RecurringType.INCOME) {
            generateIncome(rule, transactionDate);
        } else {
            generateExpense(rule, transactionDate);
        }
    }

    private void generateIncome(RecurringRule rule, LocalDateTime transactionDate) {
        Income income = Income.builder()
                .owner(rule.getOwner())
                .category(rule.getCategory())
                .price(rule.getPrice())
                .description(rule.getDescription())
                .transactionDate(transactionDate)
                .build();

        try {
            incomeService.save(income, true);
        } catch (ResourceAlreadyExistsException e) {
            log.warn("Income already generated for rule={} on date={}", rule.getId(), transactionDate.toLocalDate());
        }
    }

    private void generateExpense(RecurringRule rule, LocalDateTime transactionDate) {
        Expense expense = Expense.builder()
                .owner(rule.getOwner())
                .category(rule.getCategory())
                .price(rule.getPrice())
                .description(rule.getDescription())
                .transactionDate(transactionDate)
                .build();

        try {
            expenseService.save(expense, true, true);
        } catch (ResourceAlreadyExistsException e) {
            log.warn("Expense already generated for rule={} on date={}", rule.getId(), transactionDate.toLocalDate());
        }
    }
}
