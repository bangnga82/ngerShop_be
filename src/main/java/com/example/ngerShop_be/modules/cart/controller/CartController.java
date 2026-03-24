package com.example.ngerShop_be.modules.cart.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.cart.dto.*;
import com.example.ngerShop_be.modules.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public GlobalResponse<CartResponse> getCart(Authentication auth) {
        return cartService.getCart(requireUserId(auth));
    }

    @PostMapping("/add")
    public GlobalResponse<CartResponse> add(@RequestBody CartRequest req, Authentication auth) {
        return cartService.addToCart(req, requireUserId(auth));
    }

    @PatchMapping("/{variantId}")
    public GlobalResponse<CartResponse> update(@PathVariable UUID variantId, @RequestParam int qty, Authentication auth) {
        return cartService.updateQuantity(requireUserId(auth), variantId, qty);
    }

    @DeleteMapping("/{variantId}")
    public GlobalResponse<CartResponse> remove(@PathVariable UUID variantId, Authentication auth) {
        return cartService.removeItem(requireUserId(auth), variantId);
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id");
        }
    }
}
