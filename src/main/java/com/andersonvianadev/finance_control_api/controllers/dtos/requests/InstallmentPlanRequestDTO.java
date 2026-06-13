package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentPlanRequestDTO(
        @NotNull @Positive BigDecimal totalAmount,
        @NotNull @Min(2) Integer totalInstallments,
        @NotNull LocalDate firstDueDate,
        @NotBlank String description,
        @NotNull UUID categoryId
) {
}
