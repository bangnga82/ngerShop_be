package com.example.ngerShop_be.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_items")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Đồng nhất với Order
    UUID id;

    // productId có thể để nullable = true nếu bạn muốn lưu dự phòng,
    // nhưng quan trọng nhất vẫn là variantId
    @Column(nullable = true)
    UUID productId;

    @Column(nullable = false)
    UUID variantId;

    @Column(nullable = false)
    Integer quantity;

    @Column(nullable = false)
    Double price; // Giá chốt lúc khách nhấn đặt hàng

    @ManyToOne(fetch = FetchType.LAZY) // Cực kỳ quan trọng để tối ưu hiệu năng
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

}