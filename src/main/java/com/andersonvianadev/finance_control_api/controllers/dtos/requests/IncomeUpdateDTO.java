package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record IncomeUpdateDTO(
        @Positive(message = "the price value must be positive.")
        BigDecimal price,
        @Size(min = 1, message = "description field cannot be blank.")
        String description,
        LocalDateTime transactionDate,
        UUID categoryId
) {
}
