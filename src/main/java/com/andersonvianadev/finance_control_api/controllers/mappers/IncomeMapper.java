package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.IncomeResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public class IncomeMapper {

    public static Income toDomain(User owner, IncomeRequestDTO request) {
        return Income.builder()
                .description(request.description())
                .price(request.price())
                .transactionDate(request.transactionDate())
                .category(
                        Category.builder()
                                .id(request.categoryId())
                                .build()
                )
                .owner(owner)
                .build();
    }

    public static Income toDomain(User owner, UUID id, IncomeUpdateDTO request) {
        return Income.builder()
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

    public static IncomeResponseDTO toResponse(Income income) {
        return new IncomeResponseDTO(
                income.getId(),
                income.getTransactionDate(),
                income.getPrice(),
                income.getDescription(),
                CategoryMapper.toResponse(income.getCategory()),
                UserMapper.toResponse(income.getOwner())
        );
    }
}
