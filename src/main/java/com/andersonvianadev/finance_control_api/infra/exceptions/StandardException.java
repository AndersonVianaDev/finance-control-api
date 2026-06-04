package com.andersonvianadev.finance_control_api.infra.exceptions;

import java.time.Instant;

public record StandardException(Instant timestamp, Integer status, String error, String path) {
}
