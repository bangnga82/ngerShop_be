package com.example.ngerShop_be.modules.chatbot.util;

import com.example.ngerShop_be.modules.chatbot.dto.ChatIntent;
import com.example.ngerShop_be.modules.chatbot.dto.ParsedQuery;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ChatParser {
    private static final List<String> INTENT_KEYWORDS = List.of(
            "goi y", "tu van", "de xuat"
    );
    private static final List<String> STOPWORDS = List.of(
            "toi", "tôi", "minh", "mình", "muon", "muốn", "mua", "tim", "tìm",
            "kiem", "kiếm", "can", "cần", "lay", "lấy", "xem", "cho", "giup",
            "giúp", "voi", "với", "san", "sản", "pham", "phẩm", "gia", "giá",
            "loai", "loại", "phan", "phân", "khuc", "khúc", "khoang", "khoảng",
            "mot", "một", "nhung", "những", "cai", "cái", "lo", "lọ", "nhe",
            "nhé", "a", "à", "ah", "oi", "ơi"
    );

    private static final Pattern RANGE_PATTERN =
            Pattern.compile("tu\\s+([0-9][0-9\\s\\.]*)(k|trieu)?\\s+den\\s+([0-9][0-9\\s\\.]*)(k|trieu)?");
    private static final Pattern BELOW_PATTERN =
            Pattern.compile("duoi\\s+([0-9][0-9\\s\\.]*)(k|trieu)?");
    private static final Pattern ABOVE_PATTERN =
            Pattern.compile("tren\\s+([0-9][0-9\\s\\.]*)(k|trieu)?");

    public ParsedQuery parse(String msg, List<String> categories, List<String> colors, List<String> attributeValues) {
        if (msg == null || msg.isBlank()) {
            return new ParsedQuery(null, null, null, null, null, null, ChatIntent.UNKNOWN);
        }

        String originalMessage = msg.toLowerCase(Locale.ROOT);
        String normalizedMessage = normalize(msg);
        String category = matchValue(normalizedMessage, categories);
        String color = matchValue(normalizedMessage, colors);
        String attributeValue = matchValue(normalizedMessage, attributeValues);
        ChatIntent intent = INTENT_KEYWORDS.stream().anyMatch(normalizedMessage::contains)
                ? ChatIntent.RECOMMEND
                : ChatIntent.UNKNOWN;

        Double minPrice = null;
        Double maxPrice = null;

        Matcher range = RANGE_PATTERN.matcher(normalizedMessage);
        if (range.find()) {
            minPrice = parseNumberWithUnit(range.group(1), range.group(2));
            maxPrice = parseNumberWithUnit(range.group(3), range.group(4));
        } else {
            Matcher below = BELOW_PATTERN.matcher(normalizedMessage);
            if (below.find()) {
                maxPrice = parseNumberWithUnit(below.group(1), below.group(2));
            }

            Matcher above = ABOVE_PATTERN.matcher(normalizedMessage);
            if (above.find()) {
                minPrice = parseNumberWithUnit(above.group(1), above.group(2));
            }
        }

        String keyword = extractKeyword(originalMessage, category, color, attributeValue);
        return new ParsedQuery(category, color, attributeValue, keyword, minPrice, maxPrice, intent);
    }

    private Double parseNumberWithUnit(String number, String unit) {
        if (number == null || number.isBlank()) {
            return null;
        }
        String clean = number.replaceAll("[^0-9]", "");
        if (clean.isBlank()) {
            return null;
        }
        double value = Double.parseDouble(clean);
        if (unit == null) {
            return value;
        }
        if ("k".equals(unit)) {
            return value * 1_000;
        }
        if ("trieu".equals(unit)) {
            return value * 1_000_000;
        }
        return value;
    }

    private String normalize(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        String noDiacritics = normalized.replaceAll("\\p{M}", "");
        return noDiacritics.replace('đ', 'd');
    }

    private String matchValue(String normalizedMessage, List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<String> cleaned = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .collect(Collectors.toList());

        for (String raw : cleaned) {
            if (normalizedMessage.contains(normalize(raw))) {
                return raw;
            }
        }
        return null;
    }

    private String extractKeyword(String originalMessage, String category, String color, String attributeValue) {
        String keyword = originalMessage;
        keyword = stripTerm(keyword, category);
        keyword = stripTerm(keyword, color);
        keyword = stripTerm(keyword, attributeValue);
        keyword = keyword.replaceAll("từ\\s+[0-9][0-9\\s\\.]*\\s*(k|triệu)?\\s+đến\\s+[0-9][0-9\\s\\.]*\\s*(k|triệu)?", " ");
        keyword = keyword.replaceAll("tu\\s+[0-9][0-9\\s\\.]*\\s*(k|trieu)?\\s+den\\s+[0-9][0-9\\s\\.]*\\s*(k|trieu)?", " ");
        keyword = keyword.replaceAll("dưới\\s+[0-9][0-9\\s\\.]*\\s*(k|triệu)?", " ");
        keyword = keyword.replaceAll("duoi\\s+[0-9][0-9\\s\\.]*\\s*(k|trieu)?", " ");
        keyword = keyword.replaceAll("trên\\s+[0-9][0-9\\s\\.]*\\s*(k|triệu)?", " ");
        keyword = keyword.replaceAll("tren\\s+[0-9][0-9\\s\\.]*\\s*(k|trieu)?", " ");
        for (String intentKeyword : INTENT_KEYWORDS) {
            keyword = keyword.replace(intentKeyword, " ");
        }
        keyword = keyword.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        keyword = keyword.replaceAll("\\s+", " ").trim();
        if (keyword.isBlank()) {
            return null;
        }

        String filtered = Arrays.stream(keyword.split("\\s+"))
                .filter(token -> !STOPWORDS.contains(token))
                .filter(token -> !STOPWORDS.contains(normalize(token)))
                .collect(Collectors.joining(" "))
                .trim();

        return filtered.isBlank() ? null : filtered;
    }

    private String stripTerm(String source, String term) {
        if (term == null || term.isBlank()) {
            return source;
        }
        return source.replace(term.toLowerCase(Locale.ROOT), " ");
    }
}
