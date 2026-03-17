package com.example.ngerShop_be.common.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String error;
    private String message;
    private Instant timestamp;

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(false, error, message, Instant.now());
    }
}