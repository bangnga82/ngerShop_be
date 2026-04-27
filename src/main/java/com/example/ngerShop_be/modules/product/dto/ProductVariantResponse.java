package com.example.ngerShop_be.modules.product.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProductVariantResponse {
    UUID id;
    UUID productId;
    String productName;
    Integer stock;
    Double price;
    List<ProductAttributeResponse> attributes;
    String imageUrl;
}
