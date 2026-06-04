package com.andersonvianadev.finance_control_api.controllers.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotNull(message = "name field cannot be null.")
        String name,
        @Email(message = "email format is not valid.")
        @NotNull(message = "email field cannot be null.")
        String email,
        @Size(min = 8, max = 12, message = "the phone must be between 9 to 11 digits.")
        @NotNull(message = "password field cannot be null.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,12}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character."
        )
        String password
) {
}
