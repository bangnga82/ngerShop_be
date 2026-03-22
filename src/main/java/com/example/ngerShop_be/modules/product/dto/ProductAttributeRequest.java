package com.example.ngerShop_be.modules.product.dto;

import com.example.ngerShop_be.common.constants.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductAttributeRequest(
        @NotNull(message = "Attribute type cannot be null")
        AttributeType type,

        @NotBlank(message = "Attribute value cannot be blank")
        String value
) {
}
