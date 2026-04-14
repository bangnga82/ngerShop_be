package com.example.ngerShop_be.modules.chatbot.service;

import com.example.ngerShop_be.modules.chatbot.dto.ChatRequest;
import com.example.ngerShop_be.modules.chatbot.dto.ChatResponse;
import com.example.ngerShop_be.modules.chatbot.dto.ParsedQuery;
import com.example.ngerShop_be.modules.chatbot.util.ChatParser;
import com.example.ngerShop_be.modules.product.dto.ProductResponse;
import com.example.ngerShop_be.modules.product.entity.Product;
import com.example.ngerShop_be.modules.product.repository.CategoryRepository;
import com.example.ngerShop_be.modules.product.repository.ProductAttributeRepository;
import com.example.ngerShop_be.modules.product.repository.ProductRepository;
import com.example.ngerShop_be.modules.product.repository.ProductSpecifications;
import com.example.ngerShop_be.modules.product.util.ProductMapperUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatbotService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductMapperUtil productMapperUtil;
    private final ChatParser chatParser;

    public ChatbotService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductAttributeRepository productAttributeRepository,
                          ProductMapperUtil productMapperUtil,
                          ChatParser chatParser) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.productMapperUtil = productMapperUtil;
        this.chatParser = chatParser;
    }

    public ChatResponse handle(ChatRequest req) {
        var categories = categoryRepository.findAll().stream()
                .map(category -> category.getName())
                .toList();
        var colors = productAttributeRepository.findDistinctColorValues();
        var attributeValues = productAttributeRepository.findDistinctValues();

        ParsedQuery query = chatParser.parse(
                req == null ? null : req.message(),
                categories,
                colors,
                attributeValues
        );

        var spec = ProductSpecifications.distinct()
                .and(ProductSpecifications.isActive())
                .and(ProductSpecifications.nameContains(query.keyword()))
                .and(ProductSpecifications.categoryContains(query.category()))
                .and(ProductSpecifications.colorContains(query.color()))
                .and(ProductSpecifications.attributeValueContains(query.attributeValue()))
                .and(ProductSpecifications.minVariantPrice(query.minPrice()))
                .and(ProductSpecifications.maxVariantPrice(query.maxPrice()));

        List<Product> products = productRepository.findAll(spec, PageRequest.of(0, 5)).getContent();
        List<ProductResponse> data = products.stream()
                .map(productMapperUtil::toProductResponse)
                .toList();

        String reply = buildReply(data.size(), query);
        return new ChatResponse(reply, data);
    }

    private String buildReply(int size, ParsedQuery query) {
        return "Tim thay " + size + " san pham voi tim kiem, sau day la cac san pham de xuat cho ban.";
    }
}
