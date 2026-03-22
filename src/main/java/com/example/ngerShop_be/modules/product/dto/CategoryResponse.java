package com.example.ngerShop_be.modules.product.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String imageUrl
) {
}
