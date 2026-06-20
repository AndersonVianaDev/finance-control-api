package com.andersonvianadev.finance_control_api.infra.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalendarFeignConfig {

    @Value("${app.client.calendar-api.api-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor calendarApiAuthInterceptor() {
        return template -> template.header("Authorization", "Bearer " + apiKey);
    }
}
