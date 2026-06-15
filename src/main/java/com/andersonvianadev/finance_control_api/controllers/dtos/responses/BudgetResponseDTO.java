package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponseDTO(
        UUID id,
        UserResponseDTO user,
        CategoryResponseDTO category,
        BudgetType budgetType,
        BigDecimal limitAmount,
        Boolean active
) {
}
