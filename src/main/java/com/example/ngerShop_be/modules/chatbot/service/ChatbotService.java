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
                .map(c -> c.getName())
                .toList();
        var colors = productAttributeRepository.findDistinctColorValues();
        ParsedQuery q = chatParser.parse(req == null ? null : req.message(), categories, colors);

        var spec = ProductSpecifications.distinct()
                .and(ProductSpecifications.isActive())
                .and(ProductSpecifications.categoryContains(q.category()))
                .and(ProductSpecifications.colorContains(q.color()))
                .and(ProductSpecifications.minVariantPrice(q.minPrice()))
                .and(ProductSpecifications.maxVariantPrice(q.maxPrice()));

        List<Product> products = productRepository.findAll(spec, PageRequest.of(0, 5)).getContent();

        List<ProductResponse> data = products.stream()
                .map(productMapperUtil::toProductResponse)
                .toList();

        String reply = "Mình tìm thấy " + data.size() + " sản phẩm phù hợp.";
        return new ChatResponse(reply, data);
    }
}
