package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.RecurringRuleResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;

public class RecurringRuleMapper {

    public static RecurringRule toDomain(User owner, RecurringRuleRequestDTO request) {
        return RecurringRule.builder()
                .owner(owner)
                .transactionDate(request.transactionDate())
                .price(request.price())
                .description(request.description())
                .category(Category.builder().id(request.categoryId()).build())
                .transactionPeriodType(request.transactionPeriodType())
                .recurringType(request.recurringType())
                .build();
    }

    public static RecurringRuleResponseDTO toResponse(RecurringRule rule) {
        return new RecurringRuleResponseDTO(
                rule.getId(),
                rule.getTransactionDate(),
                rule.getPrice(),
                rule.getDescription(),
                CategoryMapper.toResponse(rule.getCategory()),
                UserMapper.toResponse(rule.getOwner()),
                rule.getTransactionPeriodType(),
                rule.getRecurringType(),
                rule.getIsActive()
        );
    }
}
