package com.andersonvianadev.finance_control_api.controllers.dtos.responses;

import java.util.UUID;

public record UserResponseDTO(UUID id, String name, String email) {
}
