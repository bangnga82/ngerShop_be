package com.example.ngerShop_be.modules.product.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProductResponse {
    UUID id;
    String name;
    String description;
    Double price;
    Integer stock;
    String creatorName;
    LocalDateTime createdDate;
    Boolean isActive;
    UUID categoryId;
    String categoryName;
    List<ProductVariantResponse> variants;
    List<ProductImageResponse> images;
}
