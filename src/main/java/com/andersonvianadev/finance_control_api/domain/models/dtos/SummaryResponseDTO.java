package com.andersonvianadev.finance_control_api.domain.models.dtos;

import java.math.BigDecimal;

public record SummaryResponseDTO(
        BigDecimal realizedIncome,
        BigDecimal realizedExpense,
        BigDecimal realizedBalance,
        long realizedCount,

        BigDecimal scheduledIncome,
        BigDecimal scheduledExpense,
        BigDecimal scheduledBalance,
        long scheduledCount,

        BigDecimal projectedIncome,
        BigDecimal projectedExpense,
        BigDecimal projectedBalance,
        long projectedCount,

        BigDecimal totalBalance,
        long totalCount,

        BigDecimal commitmentRate
) {
}
