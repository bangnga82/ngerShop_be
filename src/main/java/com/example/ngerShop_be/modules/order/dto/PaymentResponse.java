package com.example.ngerShop_be.modules.order.dto;

public record PaymentResponse(
        String paymentUrl,
        String status,
        String message
) {}