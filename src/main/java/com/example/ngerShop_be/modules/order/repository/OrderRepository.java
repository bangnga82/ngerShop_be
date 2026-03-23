package com.example.ngerShop_be.modules.order.repository;

import com.example.ngerShop_be.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    // Không cần viết @Query dài dòng nữa
    Optional<Order> findByReference(String reference);
}