package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.*;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.IncomeRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.RecurringRuleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeServiceImpl implements IIncomeService {

    private final IncomeRepository repository;
    private final ICategoryService categoryService;
    private final ICalendarService calendarService;
    private final RecurringRuleRepository recurringRuleRepository;

    @Override
    public Income save(Income income, boolean isRecurring) {
        User owner = income.getOwner();

        Category category = income.getCategory();
        UUID categoryId = category.getId();

        category = categoryService.findByIdAndOwnerOrOwnerIsNull(categoryId, owner);
        income.setCategory(category);

        if(isRecurring) {
            LocalDateTime transactionDate = income.getTransactionDate();
            CalendarDTO calendarDTO = calendarService.getDate(transactionDate.toLocalDate());

            if(!calendarDTO.isWorkingDay()) {
                transactionDate = calendarDTO.nextWorkingDay().atStartOfDay();
                income.setTransactionDate(transactionDate);
            }
        }

        boolean alreadyExists = repository.existsDuplicate(
                owner.getId(),
                categoryId,
                income.getPrice(),
                income.getTransactionDate(),
                income.getDescription()
        );

        if (alreadyExists) {
            log.warn(
                    "Attempt to create duplicate income. owner={}, category={}, date={}, price={}",
                    owner.getEmail(),
                    category.getName(),
                    income.getTransactionDate(),
                    income.getPrice()
            );
            throw new ResourceAlreadyExistsException("Income already registered");
        }

        try {
            return repository.save(income);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while creating income. owner={}, category={}, date={}, price={}",
                    owner.getEmail(),
                    category.getName(),
                    income.getTransactionDate(),
                    income.getPrice(),
                    e
            );
            throw new ResourceAlreadyExistsException("Income already registered");
        }
    }

    @Override
    public Income findById(User user, UUID id) {
        return repository.findByOwnerIdAndId(user.getId(), id)
                .orElseThrow(() -> new NotFoundException(String.format("User with id %s not found", id)));
    }

    @Override
    public Page<Income> findAll(User user, Pageable pageable) {
        return repository.findByOwnerId(user.getId(), pageable);
    }

    @Override
    public void delete(User user, UUID id) {
        Income income = this.findById(user, id);
        repository.delete(income);
    }

    @Override
    @Transactional
    public Income update(Income income) {
        User owner = income.getOwner();
        Income saved = this.findById(owner, income.getId());

        Category incomingCategory = income.getCategory();
        Category resolvedCategory = saved.getCategory();
        boolean categoryChanged = false;

        if (incomingCategory != null && incomingCategory.getId() != null) {
            resolvedCategory = categoryService.findByIdAndOwnerOrOwnerIsNull(incomingCategory.getId(), owner);
            categoryChanged = true;
        }

        BigDecimal projectedPrice = (income.getPrice() != null && !income.getPrice().equals(saved.getPrice()))
                ? income.getPrice() : saved.getPrice();
        String projectedDescription = (income.getDescription() != null && !income.getDescription().equals(saved.getDescription()))
                ? income.getDescription() : saved.getDescription();
        LocalDateTime projectedDate = (income.getTransactionDate() != null && !income.getTransactionDate().equals(saved.getTransactionDate()))
                ? income.getTransactionDate() : saved.getTransactionDate();

        boolean exists = repository.existsDuplicateExcluding(
                owner.getId(),
                resolvedCategory.getId(),
                projectedPrice,
                projectedDate,
                projectedDescription,
                saved.getId()
        );

        if (exists) {
            throw new ResourceAlreadyExistsException("Income already registered");
        }

        if (categoryChanged) saved.setCategory(resolvedCategory);
        if (!projectedPrice.equals(saved.getPrice())) saved.setPrice(projectedPrice);
        if (!projectedDescription.equals(saved.getDescription())) saved.setDescription(projectedDescription);
        if (!projectedDate.equals(saved.getTransactionDate())) saved.setTransactionDate(projectedDate);

        try {
            return repository.save(saved);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while updating income. owner={}, category={}, date={}, price={}",
                    owner.getEmail(),
                    saved.getCategory().getName(),
                    saved.getTransactionDate(),
                    saved.getPrice(),
                    e
            );
            throw new ResourceAlreadyExistsException("Income already registered");
        }
    }

    @Override
    public Page<Income> findBetweenTransactionDate(User user, LocalDateTime start, LocalDateTime finish, Pageable pageable) {
        return repository.findByOwnerIdAndTransactionDateBetween(user.getId(), start, finish, pageable);
    }

    @Override
    public BigDecimal sumByOwnerAndDateRange(User user, LocalDate start, LocalDate finish) {
        return repository.sumByOwnerAndDateRange(
                user.getId(),
                start.atStartOfDay(),
                finish.plusDays(1).atStartOfDay());
    }

    @Override
    public Integer countByOwnerAndDateRange(User user, LocalDate start, LocalDate finish) {
        return repository.countByOwnerAndDateRange(
                user.getId(),
                start.atStartOfDay(),
                finish.plusDays(1).atStartOfDay()).intValue();
    }

    @Override
    public List<Income> findProjectedIncomesByDateRange(User user, LocalDate start, LocalDate finish) {
        List<Income> projectedIncomes = new ArrayList<>();

        List<RecurringRule> recurringRules = recurringRuleRepository.findByOwnerIdAndRecurringTypeAndTransactionDateBetween(
                user.getId(), RecurringType.INCOME, start.atStartOfDay(), finish.atTime(23, 59, 59));

        for(RecurringRule recurring : recurringRules) {
            User owner = recurring.getOwner();
            Category category = recurring.getCategory();

            boolean exists = repository.existsDuplicate(
                    owner.getId(),
                    category.getId(),
                    recurring.getPrice(),
                    recurring.getTransactionDate(),
                    recurring.getDescription()
            );

            if(!exists) {
                Income income = Income.builder()
                        .transactionDate(recurring.getTransactionDate())
                        .price(recurring.getPrice())
                        .description(recurring.getDescription())
                        .owner(recurring.getOwner())
                        .category(recurring.getCategory())
                        .build();

                projectedIncomes.add(income);
            }
        }

        return projectedIncomes;
    }

    @Override
    public List<Income> findScheduledIncomesByDateRange(User user, LocalDate start, LocalDate finish) {
        return repository.findScheduledByOwnerAndDateRange(
                user.getId(),
                start.atStartOfDay(),
                finish.plusDays(1).atStartOfDay());
    }
}
