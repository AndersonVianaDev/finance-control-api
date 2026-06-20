package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IRecurringRuleService {
    RecurringRule save(RecurringRule recurringRule);
    void processRecurringOccurrences();
    RecurringRule findById(User user, UUID id);
    Page<RecurringRule> findAll(User user, Pageable pageable);
    void delete(User user, UUID id);
    RecurringRule toggle(User user, UUID id);
    RecurringRule update(RecurringRule rule);
}
