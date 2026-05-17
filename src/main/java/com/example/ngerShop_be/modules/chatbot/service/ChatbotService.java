package com.example.ngerShop_be.modules.chatbot.service;

import com.example.ngerShop_be.modules.chatbot.dto.ChatIntent;
import com.example.ngerShop_be.modules.chatbot.dto.ChatMessageDto;
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
import java.util.Objects;

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

        String message = req == null ? null : req.getMessage();
        ParsedQuery query = chatParser.parse(message, categories, colors, attributeValues);

        // Multi-turn context: only reuse constraints from recent user messages when the current message is a pure
        // refinement (e.g. "tren 150k", "duoi 200k"). If the user provides a new keyword/category/etc, treat it as a new search.
        query = enrichWithHistory(query, req == null ? null : req.getHistory(), categories, colors, attributeValues);

        if (query.intent() != null && query.intent() != ChatIntent.UNKNOWN && query.intent() != ChatIntent.RECOMMEND) {
            return new ChatResponse(buildInfoReply(query.intent()), List.of());
        }

        List<Product> products = findWithRelaxation(query);
        List<ProductResponse> data = products.stream()
                .map(productMapperUtil::toProductResponse)
                .toList();

        String reply = buildReply(data.size(), query);
        return new ChatResponse(reply, data);
    }

    private ParsedQuery enrichWithHistory(
            ParsedQuery current,
            List<ChatMessageDto> history,
            List<String> categories,
            List<String> colors,
            List<String> attributeValues
    ) {
        if (current == null) {
            current = new ParsedQuery(null, null, null, null, null, null, ChatIntent.UNKNOWN);
        }
        if (history == null || history.isEmpty()) {
            return current;
        }

        // Only inherit from history if user is refining by price only.
        boolean refineOnly = isBlank(current.keyword())
                && isBlank(current.category())
                && isBlank(current.color())
                && isBlank(current.attributeValue())
                && (current.minPrice() != null || current.maxPrice() != null);

        if (!refineOnly) {
            return current;
        }

        boolean needsAnything =
                isBlank(current.category()) ||
                        isBlank(current.color()) ||
                        isBlank(current.attributeValue()) ||
                        isBlank(current.keyword()) ||
                        current.minPrice() == null ||
                        current.maxPrice() == null;

        if (!needsAnything) {
            return current;
        }

        ParsedQuery merged = current;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageDto msg = history.get(i);
            if (msg == null) continue;
            if (!"user".equalsIgnoreCase(Objects.toString(msg.role(), ""))) continue;
            if (msg.text() == null || msg.text().isBlank()) continue;

            ParsedQuery prev = chatParser.parse(msg.text(), categories, colors, attributeValues);
            merged = mergeForContext(merged, prev);

            boolean stillNeeds =
                    isBlank(merged.category()) ||
                            isBlank(merged.color()) ||
                            isBlank(merged.attributeValue()) ||
                            isBlank(merged.keyword()) ||
                            merged.minPrice() == null ||
                            merged.maxPrice() == null;
            if (!stillNeeds) break;
        }

        return merged;
    }

    private List<Product> findWithRelaxation(ParsedQuery query) {
        // Try strict first, then relax conservatively.
        // Important: never drop a user-provided keyword; otherwise queries like "son tren 300k" would
        // degrade into a global price search and return irrelevant products.
        boolean hasKeyword = !isBlank(query.keyword());

        List<ParsedQuery> attempts = hasKeyword
                ? List.of(
                        query,
                        // Relax: drop attributeValue first (keep keyword/category/price)
                        new ParsedQuery(query.category(), query.color(), null, query.keyword(), query.minPrice(), query.maxPrice(), query.intent()),
                        // Relax: drop color as well (keep keyword/category/price)
                        new ParsedQuery(query.category(), null, null, query.keyword(), query.minPrice(), query.maxPrice(), query.intent()),
                        // Relax: drop category too (still keep keyword + price)
                        new ParsedQuery(null, null, null, query.keyword(), query.minPrice(), query.maxPrice(), query.intent())
                )
                : List.of(
                        query,
                        // If there's no keyword, allow broader relaxation to avoid returning 0 too often.
                        new ParsedQuery(query.category(), query.color(), null, null, query.minPrice(), query.maxPrice(), query.intent()),
                        new ParsedQuery(query.category(), null, null, null, query.minPrice(), query.maxPrice(), query.intent()),
                        // Global price filter (no keyword/category)
                        new ParsedQuery(null, null, null, null, query.minPrice(), query.maxPrice(), query.intent())
                );

        for (ParsedQuery attempt : attempts) {
            var spec = ProductSpecifications.distinct()
                    .and(ProductSpecifications.isActive())
                    .and(ProductSpecifications.nameContains(attempt.keyword()))
                    .and(ProductSpecifications.categoryContains(attempt.category()))
                    .and(ProductSpecifications.colorContains(attempt.color()))
                    .and(ProductSpecifications.attributeValueContains(attempt.attributeValue()))
                    .and(ProductSpecifications.minVariantPrice(attempt.minPrice()))
                    .and(ProductSpecifications.maxVariantPrice(attempt.maxPrice()));

            List<Product> products = productRepository.findAll(spec, PageRequest.of(0, 5)).getContent();
            if (products != null && !products.isEmpty()) {
                return products;
            }
        }

        // If nothing matches, return empty list.
        return List.of();
    }

    private ParsedQuery mergeForContext(ParsedQuery current, ParsedQuery previous) {
        if (previous == null) return current;

        String category = isBlank(current.category()) ? previous.category() : current.category();
        String color = isBlank(current.color()) ? previous.color() : current.color();
        String attributeValue = isBlank(current.attributeValue()) ? previous.attributeValue() : current.attributeValue();
        String keyword = isBlank(current.keyword()) ? previous.keyword() : current.keyword();
        Double minPrice = current.minPrice() == null ? previous.minPrice() : current.minPrice();
        Double maxPrice = current.maxPrice() == null ? previous.maxPrice() : current.maxPrice();

        ChatIntent intent = current.intent() == null || current.intent() == ChatIntent.UNKNOWN
                ? previous.intent()
                : current.intent();
        if (intent == null) intent = ChatIntent.UNKNOWN;

        return new ParsedQuery(category, color, attributeValue, keyword, minPrice, maxPrice, intent);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildInfoReply(ChatIntent intent) {
        return switch (intent) {
            case SHOP_INFO -> "Tên shop là: \"NgerShop\".";
            case SHOP_ADDRESS ->
                    "266 Đội Cấn, Liễu Giai, Ba Đình, Hà Nội. Bạn có thể xem địa chỉ khác ở mục Hệ thống cửa hàng tại trang chủ.";
            case CONTACT -> String.format(
                    "%s - %s. Địa chỉ: %s. Hotline: %s. Email: %s.",
                    SHOP_COMPANY, SHOP_NAME, SHOP_ADDRESS, SHOP_HOTLINE, SHOP_EMAIL
            );
            case PAYMENT_METHODS -> String.format(
                    "%s hỗ trợ đặt hàng với 2 phương thức thanh toán: COD (Thanh toán khi nhận hàng) và VN PAY. Đặt hàng tại %s.",
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
