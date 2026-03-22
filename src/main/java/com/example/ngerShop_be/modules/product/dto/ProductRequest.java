package com.example.ngerShop_be.modules.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống.")
        String name,

        String description,

        @NotNull(message = "Giá không thể rỗng")
        @Min(value = 0, message = "Giá  >= 0")
        Double price,

        @NotNull(message = "Danh mục không thể rỗng")
        UUID categoryId,

        List<MultipartFile> images
) {}
