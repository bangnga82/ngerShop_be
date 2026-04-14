package com.example.ngerShop_be.modules.payment.controller;

import com.example.ngerShop_be.common.constants.NotificationType;
import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.config.payment.VNPayConfig;
import com.example.ngerShop_be.modules.notification.service.NotificationService;
import com.example.ngerShop_be.modules.order.entity.Order;
import com.example.ngerShop_be.modules.order.repository.OrderRepository;
import com.example.ngerShop_be.modules.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PaymentController {

    final PaymentService paymentService;
    final OrderRepository orderRepository;
    final NotificationService notificationService;

    @Value("${vnpay.hashSecret}")
    String secretKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    String frontendUrl;

    @GetMapping("/vnpay")
    public GlobalResponse<String> createPayment(
            @RequestParam String orderReference,
            HttpServletRequest request) {
        String paymentUrl = paymentService.createPaymentLink(orderReference, request);
        return GlobalResponse.ok(paymentUrl);
    }

    @GetMapping("/vnpay-callback")
    public void paymentCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            fields.put(entry.getKey(), entry.getValue()[0]);
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        String signValue = VNPayConfig.hashAllFields(fields, secretKey);

        String frontendSuccessUrl = frontendUrl + "/payment-success";
        String frontendFailedUrl = frontendUrl + "/payment-failed";

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                String orderReference = request.getParameter("vnp_TxnRef");
                Order order = orderRepository.findByReference(orderReference).orElse(null);
                if (order != null) {
                    String vnpAmountParam = request.getParameter("vnp_Amount");
                    long vnpAmount;
                    try {
                        vnpAmount = Long.parseLong(vnpAmountParam) / 100;
                    } catch (NumberFormatException ex) {
                        log.warn("Invalid vnp_Amount for order {}, value {}", orderReference, vnpAmountParam);
                        response.sendRedirect(frontendFailedUrl + "?error=invalid_amount");
                        return;
                    }
                    if (Math.round(order.getTotalAmount()) == vnpAmount) {
                        if (order.getStatus() != OrderStatus.PAID) {
                            order.setStatus(OrderStatus.PAID);
                            orderRepository.save(order);
                            notificationService.sendNotification(
                                    order.getUserId(),
                                    "Thanh toan thanh cong",
                                    "Don hang " + orderReference + " da duoc thanh toan.",
                                    NotificationType.PAYMENT,
                                    order.getId().toString()
                            );
                        }
                        response.sendRedirect(frontendSuccessUrl + "?orderRef=" + orderReference);
                    } else {
                        log.warn("Invalid amount for order {}, expected {}, got {}", orderReference, order.getTotalAmount(), vnpAmount);
                        if (order.getStatus() != OrderStatus.PAID) {
                            notificationService.sendNotification(
                                    order.getUserId(),
                                    "Thanh toan that bai",
                                    "So tien thanh toan cua don hang " + orderReference + " khong khop.",
                                    NotificationType.PAYMENT,
                                    order.getId().toString()
                            );
                        }
                        response.sendRedirect(frontendFailedUrl + "?error=invalid_amount");
                    }
                } else {
                    log.warn("Order not found: {}", orderReference);
                    response.sendRedirect(frontendFailedUrl + "?error=order_not_found");
                }
            } else {
                String orderReference = request.getParameter("vnp_TxnRef");
                if (orderReference != null) {
                    Order order = orderRepository.findByReference(orderReference).orElse(null);
                    if (order != null && order.getStatus() != OrderStatus.PAID) {
                        notificationService.sendNotification(
                                order.getUserId(),
                                "Thanh toan that bai",
                                "Thanh toan cua don hang " + orderReference + " that bai hoac da bi huy.",
                                NotificationType.PAYMENT,
                                order.getId().toString()
                        );
                    }
                }
                response.sendRedirect(frontendFailedUrl);
            }
        } else {
            response.sendRedirect(frontendFailedUrl + "?error=invalid_signature");
        }
    }
}
