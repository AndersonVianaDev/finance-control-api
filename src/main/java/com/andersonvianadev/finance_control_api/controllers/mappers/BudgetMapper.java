package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.BudgetResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public class BudgetMapper {

    public static Budget toDomain(User user, BudgetRequestDTO request) {
        return Budget.builder()
                .owner(user)
                .category(Category.builder().id(request.categoryId()).build())
                .budgetType(request.budgetType())
                .limitAmount(request.limitAmount())
                .build();
    }

    public static Budget toDomain(UUID id, User user, BudgetUpdateDTO request) {
        return Budget.builder()
                .id(id)
                .owner(user)
                .budgetType(request.budgetType())
                .limitAmount(request.limitAmount())
                .active(request.active())
                .build();
    }

    public static BudgetResponseDTO toResponse(Budget budget) {
        return new BudgetResponseDTO(
                budget.getId(),
                UserMapper.toResponse(budget.getOwner()),
                CategoryMapper.toResponse(budget.getCategory()),
                budget.getBudgetType(),
                budget.getLimitAmount(),
                budget.getActive()
        );
    }
}
