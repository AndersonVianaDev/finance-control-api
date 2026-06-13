package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseResponseDTO(
        UUID id,
        LocalDateTime transactionDate,
        BigDecimal price,
        String description,
        CategoryResponseDTO category,
        Integer installmentNumber,
        Integer totalInstallments
) {
}
