package com.andersonvianadev.finance_control_api.infra.exceptions;

public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
}
