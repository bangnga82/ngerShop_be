package com.example.ngerShop_be.modules.chatbot.dto;

public record ParsedQuery(
        String category,
        String color,
        String attributeValue,
        String keyword,
        Double minPrice,
        Double maxPrice,
        ChatIntent intent
) {}
