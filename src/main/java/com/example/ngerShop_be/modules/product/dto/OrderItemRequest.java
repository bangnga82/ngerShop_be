package com.example.ngerShop_be.modules.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Variant id cannot be null")
        UUID variantId,

        @Min(value = 1, message = "Quantity must be >= 1")
        @NotNull(message = "Quantity cannot be null")
        Integer quantity
) {
}
