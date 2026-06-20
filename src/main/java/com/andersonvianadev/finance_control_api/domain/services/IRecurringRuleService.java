package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public interface IRecurringRuleService {
    RecurringRule save(RecurringRule recurringRule);
    void processRecurringOccurrences();
    RecurringRule findById(User user, UUID id);
}
