package com.example.ngerShop_be.common.exception;


import com.example.ngerShop_be.common.response.ErrorResponse;
import com.example.ngerShop_be.common.response.GlobalResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GlobalResponse.error(ErrorResponse.of("NOT_FOUND", ex.getMessage())));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest()
                .body(GlobalResponse.error(ErrorResponse.of("BAD_REQUEST", ex.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(GlobalResponse.error(ErrorResponse.of("BAD_REQUEST", ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return ResponseEntity.badRequest()
                .body(GlobalResponse.error(ErrorResponse.of("VALIDATION_ERROR", message)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(GlobalResponse.error(ErrorResponse.of("VALIDATION_ERROR", ex.getMessage())));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(GlobalResponse.error(ErrorResponse.of("UNAUTHORIZED", ex.getMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(GlobalResponse.error(ErrorResponse.of("FORBIDDEN", ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<ErrorResponse>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.error(ErrorResponse.of("INTERNAL_ERROR", "Unexpected error")));
    }
}
