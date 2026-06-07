package com.andersonvianadev.finance_control_api.infra.ratelimit;

public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long resetEpochSeconds
) {}
