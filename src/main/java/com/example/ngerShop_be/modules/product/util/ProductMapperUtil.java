package com.example.ngerShop_be.modules.product.util;

import com.example.ngerShop_be.modules.product.dto.ProductAttributeResponse;
import com.example.ngerShop_be.modules.product.dto.ProductImageResponse;
import com.example.ngerShop_be.modules.product.dto.ProductResponse;
import com.example.ngerShop_be.modules.product.dto.ProductVariantResponse;
import com.example.ngerShop_be.modules.product.entity.Product;
import com.example.ngerShop_be.modules.product.entity.ProductAttribute;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapperUtil {

    public ProductResponse toProductResponse(Product product) {
        ProductResponse response = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(calcStock(product.getVariants()))
                .creatorName(product.getCreatorName())
                .createdDate(product.getCreatedDate())
                .isActive(product.getIsActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();

        response.setImages(product.getImages().stream()
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .build())
                .toList());

        response.setVariants(product.getVariants().stream()
                .map(v -> ProductVariantResponse.builder()
                        .id(v.getId())
                        .productName(product.getName())
                        .stock(v.getStock())
                        .price(v.getPrice())
                        .imageUrl(v.getImageUrl())
                        .attributes(toAttributeResponses(v.getAttributes()))
                        .build())
                .toList());

        return response;
    }

    private List<ProductAttributeResponse> toAttributeResponses(List<ProductAttribute> attrs) {
        return attrs.stream()
                .map(a -> ProductAttributeResponse.builder()
                        .id(a.getId())
                        .type(a.getType() != null ? a.getType().name() : null)
                        .value(a.getValue())
                        .build())
                .toList();
    }

    private Integer calcStock(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) return null;
        return variants.stream().map(ProductVariant::getStock).reduce(0, Integer::sum);
    }
    public ProductAttributeResponse toAttributeResponse(ProductAttribute entity) {
        if (entity == null) return null;

        return ProductAttributeResponse.builder()
                .id(entity.getId())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .value(entity.getValue())
                .build();
    }
}
