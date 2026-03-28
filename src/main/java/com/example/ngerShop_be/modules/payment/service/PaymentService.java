package com.example.ngerShop_be.modules.payment.service;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    String createPaymentLink(String orderReference, HttpServletRequest request);
}
