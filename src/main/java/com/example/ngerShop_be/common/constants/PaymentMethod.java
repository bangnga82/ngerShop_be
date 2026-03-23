package com.example.ngerShop_be.common.constants;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod {
    MOMO,
    VN_PAY,
    COD; // Cash on delivery

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null) return null;
        try {
            return PaymentMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Trả về một mặc định hoặc quăng lỗi rõ ràng hơn
            throw new RuntimeException("Phương thức thanh toán " + value + " không hợp lệ!");
        }
    }

    @JsonValue
    public String toString() {
        return this.name();
    }

}
