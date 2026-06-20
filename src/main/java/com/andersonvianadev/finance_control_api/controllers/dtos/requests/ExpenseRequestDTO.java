package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseRequestDTO(
        @NotNull(message = "transaction date field cannot be null.")
        LocalDateTime transactionDate,
        @NotNull(message = "price field cannot be null.")
        @Positive(message = "the price value must be positive.")
        BigDecimal price,
        @NotBlank(message = "description field cannot be blank.")
        String description,
        @NotNull(message = "category field cannot be null.")
        UUID categoryId
) {
}
