package com.andersonvianadev.finance_control_api.domain.models.dtos;

import java.time.LocalDate;

public record CalendarDTO(LocalDate date, boolean isWorkingDay,
                          String reason, LocalDate nextWorkingDay) {
}
