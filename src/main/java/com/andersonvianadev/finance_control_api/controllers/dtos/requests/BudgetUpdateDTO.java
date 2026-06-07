package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetUpdateDTO(
        @NotNull(message = "limit field cannot be null.")
        @Positive(message = "the limit value must be positive.")
        BigDecimal limitAmount,
        @NotNull(message = "budget type field cannot be null.")
        BudgetType budgetType,
        @NotNull(message = "active field cannot be null.")
        Boolean active
) {
}
