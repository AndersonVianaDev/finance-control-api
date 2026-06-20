package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import com.andersonvianadev.finance_control_api.domain.models.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InstallmentPlanResponseDTO(
        UUID id,
        String description,
        BigDecimal totalAmount,
        BigDecimal installmentAmount,
        Integer totalInstallments,
        Integer paidInstallments,
        InstallmentStatus status,
        LocalDate firstDueDate,
        CategoryResponseDTO category,
        List<ExpenseResponseDTO> expenses
) {
}
