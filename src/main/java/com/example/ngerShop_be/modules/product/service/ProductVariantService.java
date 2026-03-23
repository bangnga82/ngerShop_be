package com.example.ngerShop_be.modules.product.service;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.product.dto.*;
// QUAN TRỌNG: Import OrderItemRequest từ module ORDER thay vì PRODUCT
import com.example.ngerShop_be.modules.order.dto.OrderItemRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {
    // 1. Các hàm phục vụ Controller
    GlobalResponse<ProductResponse> createVariantToProduct(ProductVariantRequest variantRequest);
    GlobalResponse<ProductResponse> updateVariantProduct(UUID variantId, ProductVariantRequest variantRequest);
    GlobalResponse<String> deleteProductVariantById(UUID variantId);
    GlobalResponse<String> uploadImageToVariant(UUID variantId, MultipartFile image);
    GlobalResponse<ProductVariantResponse> getProductVariantById(UUID variantId);

    // 2. Các hàm phục vụ OrderService

    // Giờ đây requests sẽ dùng OrderItemRequest của module Order, khớp 100% với OrderServiceImpl
    boolean checkStock(List<OrderItemRequest> requests);

    List<ProductPriceResponse> getProductPrices(List<UUID> variantIds);

    void updateStock(List<OrderItemRequest> requests);
}