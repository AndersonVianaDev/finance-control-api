package com.andersonvianadev.finance_control_api.infra.exceptions;

public class OperationNotAllowedException extends RuntimeException {
    public OperationNotAllowedException(String message) {
        super(message);
    }
}
