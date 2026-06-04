package com.andersonvianadev.finance_control_api.controllers.mappers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;

public class UserMapper {

    public static User toDomain(UserRequestDTO request) {
        return User.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .build();
    }

    public static UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail());
    }
}
