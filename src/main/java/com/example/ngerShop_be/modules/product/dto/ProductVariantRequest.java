package com.example.ngerShop_be.modules.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record ProductVariantRequest(
        @NotNull(message = "Product id cannot be null")
        UUID productId,

        @NotNull(message = "Price cannot be null")
        @Min(value = 0, message = "Price must be >= 0")
        Double price,

        @NotNull(message = "Stock cannot be null")
        @Min(value = 0, message = "Stock must be >= 0")
        Integer stock,

        MultipartFile image
) {
}
