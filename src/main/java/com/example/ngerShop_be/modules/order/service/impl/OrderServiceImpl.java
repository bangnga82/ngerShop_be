package com.example.ngerShop_be.modules.order.service.impl;

import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.common.exception.BusinessException;
import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.common.response.Status;
import com.example.ngerShop_be.modules.cart.dto.CartItemResponse;
import com.example.ngerShop_be.modules.cart.dto.CartResponse;
import com.example.ngerShop_be.modules.cart.service.CartService;
import com.example.ngerShop_be.modules.order.dto.*;
import com.example.ngerShop_be.modules.order.entity.Order;
import com.example.ngerShop_be.modules.order.entity.OrderItem;
import com.example.ngerShop_be.modules.order.repository.OrderRepository;
import com.example.ngerShop_be.modules.order.service.OrderService;
import com.example.ngerShop_be.modules.order.service.producer.OrderProducer;
import com.example.ngerShop_be.modules.order.specification.OrderSpecification;
import com.example.ngerShop_be.modules.product.dto.ProductPriceResponse;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import com.example.ngerShop_be.modules.product.repository.ProductRepository;
import com.example.ngerShop_be.modules.product.repository.ProductVariantRepository;
import com.example.ngerShop_be.modules.product.service.ProductVariantService;
import com.example.ngerShop_be.modules.product.util.GeneratorUtil;
import com.example.ngerShop_be.modules.user.entity.User;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
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
    CartService cartService;
    ProductRepository productRepository;
    ProductVariantRepository variantRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public GlobalResponse<OrderResponse> createOrder(OrderRequest request, Long userId) {
        log.info("RECEIVE ORDER CREATE: {}", request.items());
        User user = requireUser(userId);

        boolean isStockAvailable = productVariantService.checkStock(request.items());
        if (!isStockAvailable) {
            throw new BusinessException("Khong du hang trong kho.");
        }

        List<UUID> variantIds = request.items().stream()
                .map(OrderItemRequest::variantId)
                .toList();

        List<ProductPriceResponse> prices = productVariantService.getProductPrices(variantIds);

        Map<UUID, Double> priceMap = prices.stream()
                .collect(Collectors.toMap(ProductPriceResponse::variantId, ProductPriceResponse::price));

        double totalAmount = request.items().stream()
                .mapToDouble(item -> priceMap.getOrDefault(item.variantId(), 0.0) * item.quantity())
                .sum();

        Order order = Order.builder()
                .paymentMethod(request.paymentMethod())
                .reference(GeneratorUtil.generatorReference())
                .status(OrderStatus.PENDING)
                .userId(user.getId().toString())
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

        orderProducer.sendOrderConfirmation(new OrderConfirmation(
                savedOrder.getReference(),
                totalAmount,
                savedOrder.getPaymentMethod().name(),
                user.getFullName(),
                user.getEmail(),
                prices));

        orderProducer.sendUpdateStock(request.items());

        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(savedOrder, null));
    }

    @Override
    public GlobalResponse<PageResponse<OrderResponse>> findOwnOrders(Pageable pageable, OrderStatus status, Long userId) {
        Specification<Order> spec = OrderSpecification.filterOrders(status, userId.toString(), null, null, null, null, null);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return new GlobalResponse<>(Status.SUCCESS, mapToPageResponse(orders));
    }

    @Override
    public GlobalResponse<OrderResponse> findOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(order, null));
    }

    @Override
    @Transactional
    public GlobalResponse<OrderResponse> changeOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));
        order.setStatus(status);
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(orderRepository.save(order), null));
    }

    @Override
    public GlobalResponse<OrderResponse> getByReference(String reference, Long userId) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay ma don hang"));

        if (!order.getUserId().equals(userId.toString())) {
            throw new AccessDeniedException("Ban khong co quyen truy cap don hang nay");
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
        return new GlobalResponse<>(Status.SUCCESS, "Xac nhan thanh toan thanh cong");
    }

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, OrderRequest request) {
        GlobalResponse<CartResponse> cartApiResponse = cartService.getCart(userId);
        CartResponse cart = cartApiResponse.data();

        if (cart == null || cart.items().isEmpty()) {
            throw new RuntimeException("Gio hang dang trong, khong the checkout");
        }

        Order order = Order.builder()
                .reference("TECHNOVA-" + System.currentTimeMillis())
                .userId(userId.toString())
                .totalAmount(cart.totalPrice())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .addressId(request.addressId())
                .shippingFee(0.0)
                .createdDate(LocalDateTime.now())
                .build();

        for (CartItemResponse cartItem : cart.items()) {
            ProductVariant variant = variantRepository.findById(cartItem.variantId())
                    .orElseThrow(() -> new NotFoundException("San pham khong ton tai: " + cartItem.variantId()));

            if (variant.getStock() < cartItem.quantity()) {
                throw new RuntimeException("San pham " + variant.getProduct().getName() + " khong du so luong");
            }

            variant.setStock(variant.getStock() - cartItem.quantity());
            variantRepository.save(variant);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variantId(cartItem.variantId())
                    .quantity(cartItem.quantity())
                    .price(cartItem.price())
                    .build();

            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);
        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .build();
    }

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

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
