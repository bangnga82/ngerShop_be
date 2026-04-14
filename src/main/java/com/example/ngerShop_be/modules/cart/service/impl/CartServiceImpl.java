package com.example.ngerShop_be.modules.cart.service.impl;

import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.cart.dto.CartItemResponse;
import com.example.ngerShop_be.modules.cart.dto.CartRequest;
import com.example.ngerShop_be.modules.cart.dto.CartResponse;
import com.example.ngerShop_be.modules.cart.entity.Cart;
import com.example.ngerShop_be.modules.cart.entity.CartItem;
import com.example.ngerShop_be.modules.cart.repository.CartRepository;
import com.example.ngerShop_be.modules.cart.service.CartService;
import com.example.ngerShop_be.modules.product.entity.ProductAttribute;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import com.example.ngerShop_be.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    @Transactional
    public GlobalResponse<CartResponse> addToCart(CartRequest request, Long userId) {
        Cart cart = cartRepository.findCartDetailsByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).items(new ArrayList<>()).build()));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(request.variantId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + request.quantity());
        } else {
            ProductVariant variant = variantRepository.findDetailedById(request.variantId())
                    .orElseThrow(() -> new NotFoundException("San pham khong ton tai"));

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
    @Transactional(readOnly = true)
    public GlobalResponse<CartResponse> getCart(Long userId) {
        Cart cart = cartRepository.findCartDetailsByUserId(userId)
                .orElseGet(() -> Cart.builder().userId(userId).items(new ArrayList<>()).build());
        return GlobalResponse.ok(mapToCartResponse(cart));
    }

    @Override
    @Transactional
    public GlobalResponse<CartResponse> updateQuantity(Long userId, UUID variantId, int quantity) {
        Cart cart = cartRepository.findCartDetailsByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Gio hang trong"));

        cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(variantId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));

        return GlobalResponse.ok(mapToCartResponse(cartRepository.save(cart)));
    }

    @Override
    @Transactional
    public GlobalResponse<CartResponse> removeItem(Long userId, UUID variantId) {
        Cart cart = cartRepository.findCartDetailsByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Gio hang trong"));

        cart.getItems().removeIf(item -> item.getVariant().getId().equals(variantId));

        return GlobalResponse.ok(mapToCartResponse(cartRepository.save(cart)));
    }

    @Override
    @Transactional
    public GlobalResponse<String> clearCart(Long userId) {
        if (cartRepository.existsById(userId)) {
            cartRepository.deleteById(userId);
        }
        return GlobalResponse.ok("Da don sach gio hang");
    }

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
                    buildVariantLabel(item.getVariant()),
                    price,
                    item.getQuantity(),
                    subTotal,
                    item.getVariant().getImageUrl()
            ));
        }

        return new CartResponse(cart.getUserId(), itemResponses, totalPrice);
    }

    private String buildVariantLabel(ProductVariant variant) {
        if (variant.getAttributes() == null || variant.getAttributes().isEmpty()) {
            return "Default";
        }

        return variant.getAttributes().stream()
                .map(this::formatAttributeLabel)
                .collect(Collectors.joining(" / "));
    }

    private String formatAttributeLabel(ProductAttribute attribute) {
        return attribute.getType().name() + ": " + attribute.getValue();
    }
}
