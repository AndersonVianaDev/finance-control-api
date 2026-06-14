package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.ExpenseResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public class ExpenseMapper {

    public static Expense toDomain(User owner, ExpenseRequestDTO request) {
        return Expense.builder()
                .price(request.price())
                .description(request.description())
                .transactionDate(request.transactionDate())
                .owner(owner)
                .category(Category.builder()
                        .id(request.categoryId())
                        .build()
                )
                .build();
    }

    public static Expense toDomain(User owner, UUID id, ExpenseUpdateDTO request) {
        return Expense.builder()
                .id(id)
                .owner(owner)
                .price(request.price())
                .description(request.description())
                .transactionDate(request.transactionDate())
                .category(
                        Category.builder()
                                .id(request.categoryId())
                                .build()
                )
                .build();
    }

    public static ExpenseResponseDTO toResponse(Expense expense) {
        Integer totalInstallments = expense.getInstallmentPlan() != null
                ? expense.getInstallmentPlan().getTotalInstallments()
                : null;

        return new ExpenseResponseDTO(
                expense.getId(),
                expense.getTransactionDate(),
                expense.getPrice(),
                expense.getDescription(),
                CategoryMapper.toResponse(expense.getCategory()),
                expense.getInstallmentNumber(),
                totalInstallments
        );
    }
}
