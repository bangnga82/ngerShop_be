package com.example.ngerShop_be.modules.order.dto;

import com.example.ngerShop_be.modules.product.dto.ProductPriceResponse;

import java.util.List;

public record OrderConfirmation(
        String reference,
        double totalAmount,
        String paymentMethod,
        String customerName,
        String customerEmail,
        List<ProductPriceResponse> products // Để in danh sách sản phẩm vào email
) {}