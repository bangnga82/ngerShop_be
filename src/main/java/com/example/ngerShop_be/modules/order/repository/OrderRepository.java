package com.example.ngerShop_be.modules.order.repository;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    // Không cần viết @Query dài dòng nữa
    Optional<Order> findByReference(String reference);

    @Query(
            "SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi " +
                    "JOIN ProductVariant pv ON oi.variantId = pv.id " +
                    "WHERE o.userId = :userId " +
                    "AND (oi.productId = :productId OR pv.product.id = :productId) " +
                    "AND o.status = com.example.ngerShop_be.common.constants.OrderStatus.DELIVERED"
    )
    boolean hasUserPurchasedProduct(@Param("userId") Long userId,
                                    @Param("productId") UUID productId);

    long countByStatus(OrderStatus status);

    List<Order> findByCreatedDateGreaterThanEqual(LocalDateTime createdDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double sumTotalSales();
}
