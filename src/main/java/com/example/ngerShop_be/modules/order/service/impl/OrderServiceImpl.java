package com.example.ngerShop_be.modules.order.service.impl;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.common.exception.BusinessException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.common.response.Status;
import com.example.ngerShop_be.modules.order.dto.*;
import com.example.ngerShop_be.modules.order.entity.Order;
import com.example.ngerShop_be.modules.order.entity.OrderItem;
import com.example.ngerShop_be.modules.order.repository.OrderRepository;
import com.example.ngerShop_be.modules.order.service.OrderService;
import com.example.ngerShop_be.modules.order.service.producer.OrderProducer;
import com.example.ngerShop_be.modules.order.specification.OrderSpecification;
import com.example.ngerShop_be.modules.product.dto.ProductPriceResponse;
import com.example.ngerShop_be.modules.product.service.ProductVariantService;
import com.example.ngerShop_be.modules.product.util.GeneratorUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    ProductVariantService productVariantService;
    OrderProducer orderProducer;

    @Override
    @Transactional
    public GlobalResponse<OrderResponse> createOrder(OrderRequest request, Jwt jwt) {
        log.info("RECEIVE ORDER CREATE: {}", request.items());

        // 1. Kiểm tra kho trực tiếp từ ProductVariantService
        boolean isStockAvailable = productVariantService.checkStock(request.items());
        if (!isStockAvailable) {
            throw new BusinessException("Không đủ hàng trong kho.");
        }

        // 2. Lấy danh sách ID biến thể để truy vấn giá
        List<UUID> variantIds = request.items().stream()
                .map(OrderItemRequest::variantId) // Fix lỗi dòng 65 bằng cách import chuẩn DTO
                .toList();

        // 3. Lấy thông tin giá từ module Product
        List<ProductPriceResponse> prices = productVariantService.getProductPrices(variantIds);

        Map<UUID, Double> priceMap = prices.stream()
                .collect(Collectors.toMap(ProductPriceResponse::variantId, ProductPriceResponse::price));

        // 4. Tính tổng tiền đơn hàng
        double totalAmount = request.items().stream()
                .mapToDouble(item -> priceMap.getOrDefault(item.variantId(), 0.0) * item.quantity())
                .sum();

        // 5. Build và lưu Entity Order
        Order order = Order.builder()
                .paymentMethod(request.paymentMethod())
                .reference(GeneratorUtil.generatorReference())
                .status(OrderStatus.PENDING)
                .userId(jwt.getSubject())
                .totalAmount(totalAmount)
                .addressId(request.addressId())
                .notes(request.notes())
                .build();

        List<OrderItem> items = request.items().stream()
                .map(i -> OrderItem.builder()
                        .order(order)
                        .variantId(i.variantId())
                        .quantity(i.quantity())
                        .price(priceMap.get(i.variantId()))
                        .build())
                .toList();

        order.setOrderItems(items);
        Order savedOrder = orderRepository.save(order);

        // 6. Gửi thông báo qua Producer (Kafka/Async)
        orderProducer.sendOrderConfirmation(new OrderConfirmation(
                savedOrder.getReference(),
                totalAmount,
                savedOrder.getPaymentMethod().name(),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                prices));

        orderProducer.sendUpdateStock(request.items());

        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(savedOrder, null));
    }

    @Override
    public GlobalResponse<PageResponse<OrderResponse>> findOwnOrders(Pageable pageable, OrderStatus status, Jwt jwt) {
        Specification<Order> spec = OrderSpecification.filterOrders(status, jwt.getSubject(), null, null, null, null, null);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return new GlobalResponse<>(Status.SUCCESS, mapToPageResponse(orders));
    }

    @Override
    public GlobalResponse<OrderResponse> findOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(order, null));
    }

    @Override
    @Transactional
    public GlobalResponse<OrderResponse> changeOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));
        order.setStatus(status);
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(orderRepository.save(order), null));
    }

    @Override
    public GlobalResponse<OrderResponse> getByReference(String reference, Jwt jwt) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã đơn hàng"));

        if (!order.getUserId().equals(jwt.getSubject())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng này");
        }
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(order, null));
    }

    @Override
    public GlobalResponse<PageResponse<OrderResponse>> findAllOrders(
            OrderStatus status, String customerId, PaymentMethod paymentMethod,
            Double minTotal, Double maxTotal, UUID productId,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        Specification<Order> spec = OrderSpecification.filterOrders(
                status, customerId, minTotal, maxTotal, productId, startDate, endDate);

        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return new GlobalResponse<>(Status.SUCCESS, mapToPageResponse(orders));
    }

    @Override
    public GlobalResponse<String> confirmationOrder(Map<String, String> requestParams) {
        return new GlobalResponse<>(Status.SUCCESS, "Xác nhận thanh toán thành công");
    }

    // --- HELPER METHODS ---

    private OrderResponse mapToOrderResponse(Order order, PaymentResponse payment) {
        return new OrderResponse(
                order.getId(),
                order.getReference(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getOrderItems().stream()
                        .map(i -> new OrderItemResponse(i.getVariantId(), i.getQuantity(), i.getPrice()))
                        .toList(),
                payment,
                order.getCreatedDate()
        );
    }

    private PageResponse<OrderResponse> mapToPageResponse(Page<Order> page) {
        return new PageResponse<>(
                page.getContent().stream().map(o -> mapToOrderResponse(o, null)).toList(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}