package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @Email(message = "email format is not valid.")
        @NotNull(message = "email field cannot be null.")
        String email,
        @NotNull(message = "password field cannot be null.")
        String password) {
}
