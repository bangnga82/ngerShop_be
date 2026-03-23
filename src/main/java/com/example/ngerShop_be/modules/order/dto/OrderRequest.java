package com.example.ngerShop_be.modules.order.dto;

import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.modules.order.dto.OrderItemRequest;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull(message = "Danh sách sản phẩm không được trống")
        List<OrderItemRequest> items,

        @NotNull(message = "Phương thức thanh toán không được trống")
        PaymentMethod paymentMethod,

        @NotNull(message = "Địa chỉ giao hàng không được trống")
        Integer addressId, // SỬA: Trong Order Entity của bạn thường dùng String (UUID), hãy để String cho đồng bộ

        String notes,    // THÊM: Để sửa lỗi "Cannot resolve method 'notes'"

        String bankCode,
        String language
) {}