package com.example.ngerShop_be.modules.order.dto;

import java.util.UUID;

public record OrderItemRequest(
        UUID variantId,
        Integer quantity
) {}

