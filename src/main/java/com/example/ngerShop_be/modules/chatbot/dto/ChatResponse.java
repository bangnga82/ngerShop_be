package com.example.ngerShop_be.modules.chatbot.dto;

import com.example.ngerShop_be.modules.product.dto.ProductResponse;

import java.util.List;

public record ChatResponse(String reply, List<ProductResponse> products) {}
