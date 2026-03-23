package com.example.ngerShop_be.modules.order.service;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.order.dto.OrderRequest;
import com.example.ngerShop_be.modules.order.dto.OrderResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface OrderService {

    // Khách hàng đặt hàng
    GlobalResponse<OrderResponse> createOrder(OrderRequest request, Jwt jwt);

    // Khách hàng xem lịch sử đơn hàng của mình
    GlobalResponse<PageResponse<OrderResponse>> findOwnOrders(Pageable pageable, OrderStatus status, Jwt jwt);

    // Xem chi tiết một đơn hàng
    GlobalResponse<OrderResponse> findOrderById(UUID orderId);

    // Admin hoặc Hệ thống cập nhật trạng thái đơn hàng
    GlobalResponse<OrderResponse> changeOrderStatus(UUID orderId, OrderStatus status);

    // Tìm đơn hàng theo mã tham chiếu (dùng khi check thanh toán VNPay/Momo)
    GlobalResponse<OrderResponse> getByReference(String reference, Jwt jwt);

    // Admin tìm kiếm và lọc đơn hàng (Dùng Specification)
    GlobalResponse<PageResponse<OrderResponse>> findAllOrders(
            OrderStatus status,
            String customerId,
            PaymentMethod paymentMethod,
            Double minTotal,
            Double maxTotal,
            UUID productId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    // Xử lý callback sau khi thanh toán xong (Momo/VNPay)
    GlobalResponse<String> confirmationOrder(Map<String, String> requestParams);
}
