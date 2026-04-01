package com.example.ngerShop_be.modules.chatbot.controller;

import com.example.ngerShop_be.modules.chatbot.dto.ChatRequest;
import com.example.ngerShop_be.modules.chatbot.dto.ChatResponse;
import com.example.ngerShop_be.modules.chatbot.service.ChatbotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatbotService.handle(request);
    }
}
