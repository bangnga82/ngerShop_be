package com.example.ngerShop_be.common.constants;

public enum OrderStatus {
    PENDING,    // Chờ xử lý
    CONFIRMED,  // Đã xác nhận đơn
    PAID,       // Đã thanh toán
    SHIPPED,    // Đang giao hàng
    DELIVERED,  // Đã nhận hàng thành công
    CANCELLED   // Đã hủy
}