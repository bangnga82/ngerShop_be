package com.example.ngerShop_be.modules.product.dto;

import java.util.UUID;

public record ProductPriceResponse(
        UUID variantId,
        String productName,
        Integer quantity,
        double price
) {
}
