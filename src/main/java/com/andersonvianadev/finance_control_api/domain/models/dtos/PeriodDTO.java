package com.andersonvianadev.finance_control_api.domain.models.dtos;

import java.time.LocalDateTime;

public record PeriodDTO(LocalDateTime startDate, LocalDateTime endDate) {
}

