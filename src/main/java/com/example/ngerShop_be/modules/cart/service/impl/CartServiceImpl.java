package com.example.ngerShop_be.modules.cart.service.impl;

import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.cart.dto.*;
import com.example.ngerShop_be.modules.cart.entity.*;
import com.example.ngerShop_be.modules.cart.repository.CartRepository;
import com.example.ngerShop_be.modules.cart.service.CartService;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import com.example.ngerShop_be.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    @Transactional
    public GlobalResponse<CartResponse> addToCart(CartRequest request, Long userId) {
        // Tìm Cart, không có thì tạo mới
        Cart cart = cartRepository.findById(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).items(new ArrayList<>()).build()));

        // Check xem sản phẩm đã có trong giỏ chưa
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getVariant().getId().equals(request.variantId())).findFirst();

        if (existingItem.isPresent()) {
            // Có rồi thì cộng dồn số lượng
            existingItem.get().setQuantity(existingItem.get().getQuantity() + request.quantity());
        } else {
            // Chưa có thì lấy thông tin Variant và thêm mới vào List
            ProductVariant variant = variantRepository.findById(request.variantId())
                    .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.quantity())
                    .build();
            cart.getItems().add(newItem);
        }
        return GlobalResponse.ok(mapToCartResponse(cartRepository.save(cart)));
    }

    @Override
    public GlobalResponse<CartResponse> getCart(Long userId) {
        Cart cart = cartRepository.findById(userId)
                .orElseGet(() -> Cart.builder().userId(userId).items(new ArrayList<>()).build());
        return GlobalResponse.ok(mapToCartResponse(cart));
    }

    @Override
    @Transactional
    public GlobalResponse<CartResponse> updateQuantity(Long userId, UUID variantId, int quantity) {
        Cart cart = cartRepository.findById(userId).orElseThrow(() -> new NotFoundException("Giỏ hàng trống"));

        cart.getItems().stream()
                .filter(i -> i.getVariant().getId().equals(variantId))
                .findFirst()
                .ifPresent(i -> i.setQuantity(quantity));

        return GlobalResponse.ok(mapToCartResponse(cartRepository.save(cart)));
    }

    @Override
    @Transactional
    public GlobalResponse<CartResponse> removeItem(Long userId, UUID variantId) {
        Cart cart = cartRepository.findById(userId).orElseThrow(() -> new NotFoundException("Giỏ hàng trống"));

        cart.getItems().removeIf(i -> i.getVariant().getId().equals(variantId));

        return GlobalResponse.ok(mapToCartResponse(cartRepository.save(cart)));
    }

    @Override
    @Transactional
    public GlobalResponse<String> clearCart(Long userId) {
        if (cartRepository.existsById(userId)) {
            cartRepository.deleteById(userId);
        }
        return GlobalResponse.ok("Đã dọn sạch giỏ hàng");
    }

    // Hàm phụ trách tính Tổng tiền và convert sang DTO để trả về cho Frontend
    private CartResponse mapToCartResponse(Cart cart) {
        double totalPrice = 0;
        var itemResponses = new ArrayList<CartItemResponse>();

        for (CartItem item : cart.getItems()) {
            double price = item.getVariant().getPrice();
            double subTotal = price * item.getQuantity();
            totalPrice += subTotal;

            itemResponses.add(new CartItemResponse(
                    item.getVariant().getId(),
                    item.getVariant().getProduct().getName(),
                    price,
                    item.getQuantity(),
                    subTotal,
                    item.getVariant().getImageUrl()
            ));
        }
        return new CartResponse(cart.getUserId(), itemResponses, totalPrice);
    }
}
