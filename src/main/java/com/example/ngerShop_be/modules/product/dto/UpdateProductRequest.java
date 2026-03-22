package com.example.ngerShop_be.modules.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public record UpdateProductRequest(
        @NotBlank(message = "Product name cannot be blank")
        String name,

        String description,

        @NotNull(message = "Price cannot be null")
        @Min(value = 0, message = "Price must be >= 0")
        Double price,

        @NotNull(message = "Category cannot be null")
        UUID categoryId,

        List<String> existingImageUrls,

        List<MultipartFile> images
) {
}
