package com.example.ngerShop_be.modules.cart.dto;

import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        Double totalPrice
) {}
