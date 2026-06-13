package com.andersonvianadev.finance_control_api.infra.client;

import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.infra.client.api.CalendarAPI;
import com.andersonvianadev.finance_control_api.infra.client.api.CalendarExternalDTO;
import com.andersonvianadev.finance_control_api.infra.exceptions.ExternalServiceException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarClientAdapter implements ICalendarService {

    private final CalendarAPI calendarAPI;

    @Override
    public CalendarDTO getDate(LocalDate date) {
        try {
            CalendarExternalDTO response = calendarAPI.getCalendarInfo(date.toString());
            return toCalendarDTO(response);
        } catch (FeignException e) {
            throw new ExternalServiceException("calendar-api", e);
        }
    }

    private CalendarDTO toCalendarDTO(CalendarExternalDTO external) {
        LocalDate date = LocalDate.parse(external.data());
        LocalDate nextWorkingDay = external.proximoDiaUtil() != null
                ? LocalDate.parse(external.proximoDiaUtil())
                : null;
        return new CalendarDTO(date, external.diaUtilBancario(), external.motivo(), nextWorkingDay);
    }
}
