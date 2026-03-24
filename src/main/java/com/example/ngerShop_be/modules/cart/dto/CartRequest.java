package com.example.ngerShop_be.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CartRequest(
        @NotNull(message = "Variant ID không được để trống")
        UUID variantId,

        @Min(value = 1, message = "Số lượng ít nhất là 1")
        int quantity
) {}