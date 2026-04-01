package com.example.ngerShop_be.modules.chatbot.util;

import com.example.ngerShop_be.modules.chatbot.dto.ChatIntent;
import com.example.ngerShop_be.modules.chatbot.dto.ParsedQuery;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
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

    private static final Pattern RANGE_PATTERN =
            Pattern.compile("tu\\s+([0-9][0-9\\s\\.]*)(k|trieu)?\\s+den\\s+([0-9][0-9\\s\\.]*)(k|trieu)?");
    private static final Pattern BELOW_PATTERN =
            Pattern.compile("duoi\\s+([0-9][0-9\\s\\.]*)(k|trieu)?");
    private static final Pattern ABOVE_PATTERN =
            Pattern.compile("tren\\s+([0-9][0-9\\s\\.]*)(k|trieu)?");

    public ParsedQuery parse(String msg, List<String> categories, List<String> colors) {
        if (msg == null || msg.isBlank()) {
            return new ParsedQuery(null, null, null, null, ChatIntent.UNKNOWN);
        }

        String m = normalize(msg);
        String category = matchCategory(m, categories);
        String color = matchCategory(m, colors);
        ChatIntent intent = INTENT_KEYWORDS.stream().anyMatch(m::contains)
                ? ChatIntent.RECOMMEND
                : ChatIntent.UNKNOWN;

        Double minPrice = null;
        Double maxPrice = null;

        Matcher range = RANGE_PATTERN.matcher(m);
        if (range.find()) {
            minPrice = parseNumberWithUnit(range.group(1), range.group(2));
            maxPrice = parseNumberWithUnit(range.group(3), range.group(4));
            return new ParsedQuery(category, color, minPrice, maxPrice, intent);
        }

        Matcher below = BELOW_PATTERN.matcher(m);
        if (below.find()) {
            maxPrice = parseNumberWithUnit(below.group(1), below.group(2));
        }

        Matcher above = ABOVE_PATTERN.matcher(m);
        if (above.find()) {
            minPrice = parseNumberWithUnit(above.group(1), above.group(2));
        }

        return new ParsedQuery(category, color, minPrice, maxPrice, intent);
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

    private String matchCategory(String normalizedMessage, List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> cleaned = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        for (String raw : cleaned) {
            String normalizedCategory = normalize(raw);
            if (normalizedMessage.contains(normalizedCategory)) {
                return raw;
            }
        }
        return null;
    }
}
