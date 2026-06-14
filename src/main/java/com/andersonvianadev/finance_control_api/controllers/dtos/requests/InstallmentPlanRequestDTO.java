package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentPlanRequestDTO(
        @NotNull(message = "total amount field cannot be null.")
        @Positive(message = "the total amount must be positive.")
        BigDecimal totalAmount,
        @NotNull(message = "total installments field cannot be null.")
        @Min(value = 2, message = "minimum 2 installments required.")
        Integer totalInstallments,
        @NotNull(message = "first due date field cannot be null.")
        LocalDate firstDueDate,
        @NotBlank(message = "description field cannot be blank.")
        String description,
        @NotNull(message = "category field cannot be null.")
        UUID categoryId
) {
}
