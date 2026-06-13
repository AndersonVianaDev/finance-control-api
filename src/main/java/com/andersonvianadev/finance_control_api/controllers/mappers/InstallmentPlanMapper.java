package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.InstallmentPlanRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.InstallmentPlanResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InstallmentPlanMapper {

    public static InstallmentPlan toDomain(User owner, InstallmentPlanRequestDTO request) {
        BigDecimal installmentAmount = request.totalAmount()
                .divide(BigDecimal.valueOf(request.totalInstallments()), 2, RoundingMode.HALF_UP);

        return InstallmentPlan.builder()
                .owner(owner)
                .category(Category.builder().id(request.categoryId()).build())
                .description(request.description())
                .totalAmount(request.totalAmount())
                .installmentAmount(installmentAmount)
                .totalInstallments(request.totalInstallments())
                .firstDueDate(request.firstDueDate())
                .build();
    }

    public static InstallmentPlanResponseDTO toResponse(InstallmentPlan plan, Expense firstExpense) {
        return new InstallmentPlanResponseDTO(
                plan.getId(),
                plan.getDescription(),
                plan.getTotalAmount(),
                plan.getInstallmentAmount(),
                plan.getTotalInstallments(),
                plan.getPaidInstallments(),
                plan.getStatus(),
                plan.getFirstDueDate(),
                CategoryMapper.toResponse(plan.getCategory()),
                ExpenseMapper.toResponse(firstExpense)
        );
    }
}
