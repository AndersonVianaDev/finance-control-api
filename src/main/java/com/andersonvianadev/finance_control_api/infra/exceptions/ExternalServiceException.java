package com.andersonvianadev.finance_control_api.infra.exceptions;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String serviceName, Throwable cause) {
        super("External service unavailable: " + serviceName, cause);
    }
}
