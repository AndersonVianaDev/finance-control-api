package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.infra.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Objects;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<StandardException> resourceAlreadyExistsException(ResourceAlreadyExistsException e, HttpServletRequest request) {
        StandardException exception = new StandardException(Instant.now(), HttpStatus.CONFLICT.value(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardException> validationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        StandardException exception = new StandardException(Instant.now(), HttpStatus.BAD_REQUEST.value(), message, request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<StandardException> notFoundException(NotFoundException e, HttpServletRequest request) {
        StandardException exception = new StandardException(Instant.now(), HttpStatus.NOT_FOUND.value(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardException> badCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        String message = "Username does not exist or password is invalid.";
        StandardException exception = new StandardException(Instant.now(), HttpStatus.UNAUTHORIZED.value(), message, request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<StandardException> quotaExceededException(QuotaExceededException e, HttpServletRequest request) {
        StandardException exception = new StandardException(Instant.now(), HttpStatus.FORBIDDEN.value(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardException> accessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        StandardException exception = new StandardException(Instant.now(), HttpStatus.FORBIDDEN.value(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<StandardException> tokenException(TokenException e, HttpServletRequest request) {
        String message = "Invalid or expired token";
        StandardException exception = new StandardException(Instant.now(), HttpStatus.UNAUTHORIZED.value(), message, request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardException> exceptionGeneric(Exception e, HttpServletRequest request) {
        StandardException exception = new StandardException(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(exception);
    }

}
