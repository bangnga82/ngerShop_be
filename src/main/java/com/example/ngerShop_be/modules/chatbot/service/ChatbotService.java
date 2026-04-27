package com.example.ngerShop_be.modules.chatbot.service;

import com.example.ngerShop_be.modules.chatbot.dto.ChatRequest;
import com.example.ngerShop_be.modules.chatbot.dto.ChatIntent;
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
    private static final String SHOP_NAME = "NgerShop";
    private static final String SHOP_COMPANY = "My pham NgerShop";
    private static final String SHOP_ADDRESS = "266 Doi Can, Lieu Giai, Ba Dinh, Ha Noi";
    private static final String SHOP_EMAIL = "bangnga@gmail.com";
    private static final String SHOP_HOTLINE = "0362648200";
    private static final String STORE_SYSTEM_PATH = "/market-system";
    private static final String CONTACT_PATH = "/contact";
    private static final String ORDER_PATH = "/order";

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

        if (query.intent() != null && query.intent() != ChatIntent.UNKNOWN && query.intent() != ChatIntent.RECOMMEND) {
            return new ChatResponse(buildInfoReply(query.intent()), List.of());
        }

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

    private String buildInfoReply(ChatIntent intent) {
        return switch (intent) {
            case SHOP_INFO -> "Tên shop là: \"NgerShop\".";
            case SHOP_ADDRESS -> "266 Đội Cấn, Liễu Giai, Ba Đình, Hà Nội. Bạn có thể xem địa chỉ khác ở mục Hệ thống cửa hàng tại trang chủ.";
            case CONTACT -> String.format(
                    "%s - %s. Dia chi: %s. Hotline: %s. Email: %s.",
                    SHOP_COMPANY, SHOP_NAME, SHOP_ADDRESS, SHOP_HOTLINE, SHOP_EMAIL
            );
            case PAYMENT_METHODS -> String.format(
                    "%s Hỗ trợ đặt hàng với 2 phương thức thanh toán: COD (Thanh toán khi nhận hàng) và VN PAY. Đặt hàng tại %s.",
                    SHOP_NAME, ORDER_PATH
            );
            case ORDER_GUIDE -> String.format(
                    "Cách đặt hàng tại %s: chọn sản phẩm -> Thêm vào giỏ hàng -> vào trang đặt hàng -> chọn địa chỉ giao hàng -> chọn COD hoặc VN PAY -> Đặt hàng.",
                    SHOP_NAME
            );
            default -> "Bạn muốn hỏi thông tin gì về shop?";
        };
    }

    private String buildReply(int size, ParsedQuery query) {
        return "Tìm thấy " + size + " sản phẩm với tìm kiếm, sau đây là các sản phẩm đề xuất với bạn.";
    }
}
