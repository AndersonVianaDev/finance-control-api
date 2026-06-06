package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.CategoryRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.CategoryResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;

public class CategoryMapper {

    public static Category toDomain(User user, CategoryRequestDTO request) {
        return Category.builder()
                .owner(user)
                .name(request.name())
                .description(request.description())
                .icon(request.icon())
                .build();
    }

    public static CategoryResponseDTO toResponse(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                UserMapper.toResponse(category.getOwner())
        );
    }
}
