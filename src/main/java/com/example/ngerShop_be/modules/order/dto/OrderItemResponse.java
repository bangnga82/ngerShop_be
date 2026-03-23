package com.example.ngerShop_be.modules.order.dto;

import java.util.UUID;

public record OrderItemResponse(
        UUID variantId,
        Integer quantity,
        Double price
) {}