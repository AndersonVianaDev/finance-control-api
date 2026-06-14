package com.andersonvianadev.finance_control_api.infra.exceptions;

public class DeleteNotAllowedException extends RuntimeException {
    public DeleteNotAllowedException(String message) {
        super(message);
    }
}
