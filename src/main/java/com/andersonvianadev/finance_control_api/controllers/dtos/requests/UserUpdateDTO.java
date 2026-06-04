package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.Email;

public record UserUpdateDTO(
        String name,
        @Email(message = "email format is not valid.")
        String email) {
}
