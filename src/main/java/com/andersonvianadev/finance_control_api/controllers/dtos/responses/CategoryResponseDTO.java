package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        String name,
        String description,
        String icon,
        UserResponseDTO owner
) {
}
