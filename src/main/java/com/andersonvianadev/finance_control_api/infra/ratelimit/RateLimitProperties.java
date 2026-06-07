package com.andersonvianadev.finance_control_api.infra.ratelimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Global global,
        Auth auth
) {

    public record Global(
            @Min(1) int requestsPerMinute
    ) {}

    public record Auth(
            @Min(1) int requestsPerMinute
    ) {}
}
