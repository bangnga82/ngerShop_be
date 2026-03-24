package com.example.ngerShop_be.modules.order.dto;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse { // Chuyển thành CLASS
    UUID id;
    String reference;
    OrderStatus status;
    PaymentMethod paymentMethod;
    Double totalAmount;
    List<OrderItemResponse> items;
    Object paymentDetails;
    LocalDateTime createdDate;
}