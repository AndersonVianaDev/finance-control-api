package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequestDTO(
        @NotNull(message = "category field cannot be null.")
        UUID categoryId,
        @NotNull(message = "budget type field cannot be null.")
        BudgetType budgetType,
        @NotNull(message = "limit field cannot be null.")
        @Positive(message = "the limit value must be positive.")
        BigDecimal limitAmount
) {
}
