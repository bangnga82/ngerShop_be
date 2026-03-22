package com.example.ngerShop_be.modules.product.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PurchaseRequest(
        List<UUID> variantIds,
        Map<UUID, Integer> orderedQuantities
) {
}
