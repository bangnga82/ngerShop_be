package com.example.ngerShop_be.modules.order.controller;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.order.dto.OrderRequest;
import com.example.ngerShop_be.modules.order.dto.OrderResponse;
import com.example.ngerShop_be.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 1. Khách hàng đặt hàng mới
     */
    @PostMapping
    public GlobalResponse<OrderResponse> createOrder(
            @RequestBody @Valid OrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.createOrder(request, jwt);
    }

    /**
     * 2. Khách hàng xem lịch sử đơn hàng của chính mình
     */
    @GetMapping("/my-orders")
    public GlobalResponse<PageResponse<OrderResponse>> getMyOrders(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.findOwnOrders(pageable, status, jwt);
    }

    /**
     * 3. Xem chi tiết đơn hàng theo ID
     */
    @GetMapping("/{orderId}")
    public GlobalResponse<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        return orderService.findOrderById(orderId);
    }

    /**
     * 4. Tìm đơn hàng nhanh qua mã Reference (Ví dụ: TECH-123456)
     */
    @GetMapping("/reference/{reference}")
    public GlobalResponse<OrderResponse> getByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.getByReference(reference, jwt);
    }

    /**
     * 5. ADMIN: Quản lý và lọc toàn bộ đơn hàng của hệ thống
     * Sử dụng OrderSpecification bạn đã viết để lọc theo ngày, giá, sản phẩm...
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalResponse<PageResponse<OrderResponse>> findAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Double minTotal,
            @RequestParam(required = false) Double maxTotal,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return orderService.findAllOrders(status, customerId, paymentMethod, minTotal, maxTotal, productId, startDate, endDate, pageable);
    }

    /**
     * 6. ADMIN: Cập nhật trạng thái đơn hàng (Duyệt đơn, Giao hàng, Hủy đơn)
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalResponse<OrderResponse> changeStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status
    ) {
        return orderService.changeOrderStatus(orderId, status);
    }

    /**
     * 7. Callback xử lý kết quả thanh toán trực tuyến
     */
    @GetMapping("/payment-confirmation")
    public GlobalResponse<String> paymentConfirmation(@RequestParam Map<String, String> params) {
        return orderService.confirmationOrder(params);
    }
}