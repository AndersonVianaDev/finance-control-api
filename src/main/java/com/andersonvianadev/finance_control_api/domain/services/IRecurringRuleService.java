package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;

public interface IRecurringRuleService {
    RecurringRule save(RecurringRule recurringRule);
    void processRecurringOccurrences();
}
