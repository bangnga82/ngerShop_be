package com.example.ngerShop_be.modules.cart.repository;

import com.example.ngerShop_be.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
            select distinct c
            from Cart c
            left join fetch c.items i
            left join fetch i.variant v
            left join fetch v.product
            where c.userId = :userId
            """)
    Optional<Cart> findCartDetailsByUserId(Long userId);
}
