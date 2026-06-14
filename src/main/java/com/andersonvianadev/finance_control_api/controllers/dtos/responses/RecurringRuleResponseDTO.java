package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.models.enums.TransactionPeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecurringRuleResponseDTO(
        UUID id,
        LocalDateTime transactionDate,
        BigDecimal price,
        String description,
        CategoryResponseDTO category,
        UserResponseDTO owner,
        TransactionPeriodType transactionPeriodType,
        RecurringType recurringType,
        Boolean isActive
) {
}
