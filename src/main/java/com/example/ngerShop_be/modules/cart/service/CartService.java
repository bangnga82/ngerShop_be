package com.example.ngerShop_be.modules.cart.service;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.cart.dto.CartRequest;
import com.example.ngerShop_be.modules.cart.dto.CartResponse;

import java.util.UUID;

public interface CartService {
    // 1. Thêm vào giỏ (Nếu trùng variantId thì cộng dồn quantity)
    GlobalResponse<CartResponse> addToCart(CartRequest cartRequest, Long userId);

    // 2. Lấy giỏ hàng của User
    GlobalResponse<CartResponse> getCart(Long userId);

    // 3. Xóa 1 sản phẩm khỏi giỏ
    GlobalResponse<CartResponse> removeItem(Long userId, UUID variantId);

    // 4. Xóa sạch giỏ hàng (Clear)
    GlobalResponse<String> clearCart(Long userId);

    // 5. Cập nhật số lượng của 1 sản phẩm
    GlobalResponse<CartResponse> updateQuantity(Long userId, UUID variantId, int quantity);
}
