package com.example.ngerShop_be.modules.cart.dto;

import java.util.UUID;

public record CartItemResponse(
        UUID variantId,
        String productName,
        String variantLabel,
        Double price,
        Integer quantity,
        Double subTotal,
        String imageUrl
) {}
