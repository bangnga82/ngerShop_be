package com.example.ngerShop_be.modules.order.entity;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "orders")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Dùng UUID chuẩn hơn AUTO
    UUID id;

    @Column(unique = true, nullable = false)
    String reference;

    Integer addressId;

    Double totalAmount; // Tổng thanh toán (Tiền hàng + Phí ship)

    Double shippingFee; // Phí vận chuyển

    @Enumerated(value = EnumType.STRING)
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    OrderStatus status;

    // Nếu đã cấu hình AuditorAware thì dùng @CreatedBy,
    // nếu chưa thì nên bỏ @CreatedBy để set thủ công từ JWT trong Service
    @Column(nullable = false, updatable = false)
    String userId;

    @Column(columnDefinition = "TEXT")
    String notes;

    @Builder.Default // QUAN TRỌNG: Ngăn builder làm list bị null
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderItem> orderItems = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    LocalDateTime lastModifiedDate;

    // --- HÀM HELPER BẮT BUỘC PHẢI CÓ ---
    public void addOrderItem(OrderItem item) {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        orderItems.add(item);
        item.setOrder(this); // Gán ngược lại để JPA hiểu mối quan hệ
    }
}