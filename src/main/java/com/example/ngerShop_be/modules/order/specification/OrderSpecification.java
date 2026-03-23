package com.example.ngerShop_be.modules.order.specification;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.modules.order.entity.Order;
import com.example.ngerShop_be.modules.order.entity.OrderItem;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderSpecification {

    public static Specification<Order> filterOrders(
            OrderStatus status,
            String userId,
            Double minTotal,
            Double maxTotal,
            UUID productId, // Lọc theo sản phẩm
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo trạng thái
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 2. Lọc theo User ID
            if (userId != null && !userId.isEmpty()) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            // 3. Lọc theo khoảng giá
            if (minTotal != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minTotal));
            }
            if (maxTotal != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxTotal));
            }

            // 4. Lọc theo Product ID (Join sang bảng OrderItem)
            if (productId != null) {
                Join<Order, OrderItem> orderItemsJoin = root.join("orderItems");
                predicates.add(cb.equal(orderItemsJoin.get("productId"), productId));
                query.distinct(true); // Tránh bị trùng lặp dòng khi Join
            }

            // 5. Lọc theo khoảng ngày tạo
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}