package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseRequestDTO(
        LocalDateTime transactionDate,
        BigDecimal price,
        String description,
        UUID categoryId
) {
}
