package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;

import java.time.LocalDate;

public interface ICalendarService {
    CalendarDTO getDate(LocalDate date);
}
