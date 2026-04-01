package com.example.ngerShop_be.modules.chatbot.dto;

public record ParsedQuery(String category, String color, Double minPrice, Double maxPrice, ChatIntent intent) {}
