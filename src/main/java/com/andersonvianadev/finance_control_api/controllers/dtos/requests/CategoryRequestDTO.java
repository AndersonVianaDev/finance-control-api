package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
        @NotNull(message = "name field cannot be null.")
        @Size(max = 30, message = "Name field must contain at most 30 characters.")
        String name,
        @Size(max = 50, message = "Description field must contain at most 50 characters.")
        String description,
        @Size(max = 20, message = "Description field must contain at most 20 characters.")
        String icon
) {
}
