package com.example.ngerShop_be.modules.cart.repository;

import com.example.ngerShop_be.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    void deleteByVariant_IdIn(List<UUID> variantIds);
}
