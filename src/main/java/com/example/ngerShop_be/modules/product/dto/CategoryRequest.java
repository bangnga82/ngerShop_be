package com.example.ngerShop_be.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CategoryRequest(
        @NotBlank(message = "Tên danh mục không được để trống.")
        @Size(min = 3, max = 50, message = "Tên danh mục phải có độ dài từ 3 đến 50 ký tự.")
        String name,

        @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự.")
        String description,

        MultipartFile image
) {
}
