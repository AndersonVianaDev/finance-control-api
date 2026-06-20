package com.andersonvianadev.finance_control_api.infra.exceptions;

public class BudgetExceededException extends RuntimeException {
    public BudgetExceededException(String message) {
        super(message);
    }
}
