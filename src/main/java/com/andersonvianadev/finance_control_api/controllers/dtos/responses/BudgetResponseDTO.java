package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;

import java.math.BigDecimal;

public record BudgetResponseDTO(
        UserResponseDTO user,
        CategoryResponseDTO category,
        BudgetType budgetType,
        BigDecimal limitAmount,
        Boolean active
) {
}
