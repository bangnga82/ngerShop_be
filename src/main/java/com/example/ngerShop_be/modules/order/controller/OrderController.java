package com.example.ngerShop_be.modules.order.controller;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.common.exception.BadRequestException;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public GlobalResponse<OrderResponse> createOrder(
            @RequestBody @Valid OrderRequest request,
            Authentication auth
    ) {
        return orderService.createOrder(request, requireUserId(auth));
    }

    @GetMapping("/my-orders")
    public GlobalResponse<PageResponse<OrderResponse>> getMyOrders(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) OrderStatus status,
            Authentication auth
    ) {
        return orderService.findOwnOrders(pageable, status, requireUserId(auth));
    }

    @GetMapping("/{orderId}")
    public GlobalResponse<OrderResponse> getOrderById(@PathVariable UUID orderId, Authentication auth) {
        Long userId = requireUserId(auth);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return orderService.findOrderById(orderId, userId, isAdmin);
    }

    @GetMapping("/reference/{reference}")
    public GlobalResponse<OrderResponse> getByReference(
            @PathVariable String reference,
            Authentication auth
    ) {
        return orderService.getByReference(reference, requireUserId(auth));
    }

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

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public GlobalResponse<OrderResponse> changeStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status
    ) {
        return orderService.changeOrderStatus(orderId, status);
    }

    @GetMapping("/payment-confirmation")
    public GlobalResponse<String> paymentConfirmation(@RequestParam Map<String, String> params) {
        return orderService.confirmationOrder(params);
    }

    @PostMapping("/checkout")
    public GlobalResponse<OrderResponse> checkout(
            Authentication auth,
            @RequestBody @Valid OrderRequest request
    ) {
        OrderResponse result = orderService.checkout(requireUserId(auth), request);
        return GlobalResponse.ok(result);
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthorized");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid user id");
        }
    }
}
