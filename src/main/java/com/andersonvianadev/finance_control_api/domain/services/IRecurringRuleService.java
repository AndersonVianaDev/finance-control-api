package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IRecurringRuleService {
    RecurringRule save(RecurringRule recurringRule);
    void processRecurringOccurrences();
    RecurringRule findById(User user, UUID id);
    Page<RecurringRule> findAll(User user, Pageable pageable);
    void delete(User user, UUID id);
    RecurringRule toggle(User user, UUID id);
    RecurringRule update(RecurringRule rule);
    List<RecurringRule> findByRangeDateAndType(User user, LocalDate start, LocalDate finish, RecurringType type);
}
