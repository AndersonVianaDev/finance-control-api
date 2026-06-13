package com.andersonvianadev.finance_control_api.infra.client.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "calendar-api", url = "${app.client.url.calendar-api}")
public interface CalendarAPI {

    @GetMapping("/dia-util-bancario/{date}")
    CalendarExternalDTO getCalendarInfo(@PathVariable String date);
}
