package com.example.ngerShop_be.common.constants;

import lombok.Getter;

@Getter
public enum AttributeType {
    COLOR("color"),
    SIZE("size"),
    MATERIAL("material"),  // Chất liệu (cotton, da, polyester, ...)
    STORAGE("storage"),    // Dung lượng (128GB, 256GB, ...)
    RAM("ram"),            // Bộ nhớ RAM (8GB, 16GB, ...)
    WEIGHT("weight");      // Trọng lượng (500g, 1kg, ...)

    private final String value;


    AttributeType(String value) {
        this.value = value;
    }
}
