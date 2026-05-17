package com.example.ngerShop_be.modules.order.service.impl;

import com.example.ngerShop_be.common.constants.NotificationType;
import com.example.ngerShop_be.common.constants.OrderStatus;
import com.example.ngerShop_be.common.constants.PaymentMethod;
import com.example.ngerShop_be.common.exception.BadRequestException;
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
import com.example.ngerShop_be.modules.user.entity.Address;
import com.example.ngerShop_be.modules.user.repository.AddressRepository;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import com.example.ngerShop_be.modules.notification.service.NotificationService;
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
    AddressRepository addressRepository;
    NotificationService notificationService;

    @Override
    @Transactional
    public GlobalResponse<OrderResponse> createOrder(OrderRequest request, Long userId) {
        log.info("RECEIVE ORDER CREATE: {}", request.items());
        User user = requireUser(userId);
        Address address = requireAddress(user, request.addressId());

        boolean isStockAvailable = productVariantService.checkStock(request.items());
        if (!isStockAvailable) {
            throw new BusinessException("Không đủ hàng trong kho.");
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

        // Trừ kho ngay trong transaction (đảm bảo không bị âm)
        productVariantService.updateStock(request.items());

        Order order = Order.builder()
                .paymentMethod(request.paymentMethod())
                .reference(GeneratorUtil.generatorReference())
                .status(OrderStatus.PENDING)
                .userId(user.getId())
                .totalAmount(totalAmount)
                .addressId(request.addressId())
                .recipientName(resolveRecipientName(user))
                .recipientPhone(resolveRecipientPhone(user, address))
                .deliveryAddress(buildDeliveryAddress(address))
                .notes(request.notes())
                .build();

        List<OrderItem> items = request.items().stream()
                .map(i -> {
                    UUID variantId = i.variantId();
                    ProductVariant variant = variantRepository.findById(variantId)
                            .orElseThrow(() -> new NotFoundException("Khong tim thay bien the: " + variantId));
                    return OrderItem.builder()
                            .order(order)
                            .productId(variant.getProduct().getId())
                            .variantId(variantId)
                            .quantity(i.quantity())
                            .price(priceMap.get(variantId))
                            .build();
                })
                .toList();

        order.setOrderItems(items);
        Order savedOrder = orderRepository.save(order);
        notificationService.sendNotification(
                user.getId(),
                "Đặt hàng thành công",
                "Đơn hàng " + savedOrder.getReference() + " đã đươc tạo.",
                NotificationType.ORDER,
                savedOrder.getId().toString()
        );

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
        Specification<Order> spec = OrderSpecification.filterOrders(status, userId, null, null, null, null, null);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return new GlobalResponse<>(Status.SUCCESS, mapToPageResponse(orders));
    }

    @Override
    public GlobalResponse<OrderResponse> findOrderById(UUID orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));
        if (!isAdmin && (userId == null || !order.getUserId().equals(userId))) {
            throw new AccessDeniedException("Ban khong co quyen truy cap don hang nay");
        }
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(order, null));
    }

    @Override
    @Transactional
    public GlobalResponse<OrderResponse> changeOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay don hang"));
        if (status == OrderStatus.SHIPPED && order.getShippedAt() == null) {
            // Persist the moment the order enters shipping so we can compute ETA consistently.
            order.setShippedAt(LocalDateTime.now());
        }
        if (status == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            // Persist the moment the order is marked delivered (actual delivered date).
            order.setDeliveredAt(LocalDateTime.now());
        }
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        notificationService.sendNotification(
                savedOrder.getUserId(),
                "Cap nhat trang thai don hang",
                "Don hang " + savedOrder.getReference() + " da chuyen sang trang thai " + savedOrder.getStatus() + ".",
                NotificationType.ORDER,
                savedOrder.getId().toString()
        );
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(savedOrder, null));
    }

    @Override
    public GlobalResponse<OrderResponse> getByReference(String reference, Long userId) {
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay ma don hang"));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Ban khong co quyen truy cap don hang nay");
        }
        return new GlobalResponse<>(Status.SUCCESS, mapToOrderResponse(order, null));
    }

    @Override
    public GlobalResponse<PageResponse<OrderResponse>> findAllOrders(
            OrderStatus status, String customerId, PaymentMethod paymentMethod,
            Double minTotal, Double maxTotal, UUID productId,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        Long customerIdValue = null;
        if (customerId != null && !customerId.isBlank()) {
            try {
                customerIdValue = Long.parseLong(customerId.trim());
            } catch (NumberFormatException ex) {
                throw new BadRequestException("customerId must be a number");
            }
        }

        Specification<Order> spec = OrderSpecification.filterOrders(
                status, customerIdValue, minTotal, maxTotal, productId, startDate, endDate);

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

        User user = requireUser(userId);
        Address address = requireAddress(user, request.addressId());

        Order order = Order.builder()
                .reference("TECHNOVA-" + System.currentTimeMillis())
                .userId(userId)
                .totalAmount(cart.totalPrice())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .addressId(request.addressId())
                .recipientName(resolveRecipientName(user))
                .recipientPhone(resolveRecipientPhone(user, address))
                .deliveryAddress(buildDeliveryAddress(address))
                .shippingFee(0.0)
                .createdDate(LocalDateTime.now())
                .build();

        List<OrderItemRequest> stockRequests = cart.items().stream()
                .map(i -> new OrderItemRequest(i.variantId(), i.quantity()))
                .toList();
        productVariantService.updateStock(stockRequests);

        for (CartItemResponse cartItem : cart.items()) {
            ProductVariant variant = variantRepository.findById(cartItem.variantId())
                    .orElseThrow(() -> new NotFoundException("San pham khong ton tai: " + cartItem.variantId()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(variant.getProduct().getId())
                    .variantId(cartItem.variantId())
                    .quantity(cartItem.quantity())
                    .price(cartItem.price())
                    .build();

            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        notificationService.sendNotification(
                userId,
                "Dat hang thanh cong",
                "Don hang " + savedOrder.getReference() + " da duoc tao.",
                NotificationType.ORDER,
                savedOrder.getId().toString()
        );
        cartService.clearCart(userId);
        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        EstimatedDelivery eta = computeEstimatedDelivery(order);
        return OrderResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .addressId(order.getAddressId())
                .recipient(resolveOrderRecipient(order))
                .recipientPhone(resolveOrderRecipientPhone(order))
                .deliveryAddress(resolveOrderDeliveryAddress(order))
                .shippedAt(order.getShippedAt())
                .deliveredAt(resolveDeliveredAt(order))
                .estimatedDeliveryFrom(eta.from())
                .estimatedDeliveryTo(eta.to())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order, PaymentResponse payment) {
        EstimatedDelivery eta = computeEstimatedDelivery(order);
        List<UUID> variantIds = order.getOrderItems().stream()
                .map(OrderItem::getVariantId)
                .toList();

        Map<UUID, ProductVariant> variantsById = variantRepository
                .findAllWithProductByIdIn(variantIds)
                .stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v, (a, b) -> a));

        return OrderResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .addressId(order.getAddressId())
                .recipient(resolveOrderRecipient(order))
                .recipientPhone(resolveOrderRecipientPhone(order))
                .deliveryAddress(resolveOrderDeliveryAddress(order))
                .shippedAt(order.getShippedAt())
                .deliveredAt(resolveDeliveredAt(order))
                .estimatedDeliveryFrom(eta.from())
                .estimatedDeliveryTo(eta.to())
                .items(order.getOrderItems().stream()
                        .map(i -> {
                            ProductVariant variant = variantsById.get(i.getVariantId());
                            String productName = variant != null && variant.getProduct() != null
                                    ? variant.getProduct().getName()
                                    : null;
                            String imageUrl = variant != null ? variant.getImageUrl() : null;
                            return new OrderItemResponse(
                                    i.getVariantId(),
                                    i.getQuantity(),
                                    i.getPrice(),
                                    productName,
                                    imageUrl
                            );
                        })
                        .toList())
                .paymentDetails(payment)
                .createdDate(order.getCreatedDate())
                .build();
    }

    private record EstimatedDelivery(LocalDateTime from, LocalDateTime to) {}

    private EstimatedDelivery computeEstimatedDelivery(Order order) {
        if (order == null) return new EstimatedDelivery(null, null);

        LocalDateTime base = order.getShippedAt();
        if (base == null && order.getStatus() != null) {
            // Fallback for older orders created before shippedAt existed.
            if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
                base = order.getLastModifiedDate() != null ? order.getLastModifiedDate() : order.getCreatedDate();
            }
        }
        if (base == null) return new EstimatedDelivery(null, null);

        return new EstimatedDelivery(base.plusDays(3), base.plusDays(5));
    }

    private LocalDateTime resolveDeliveredAt(Order order) {
        if (order == null) return null;
        if (order.getDeliveredAt() != null) return order.getDeliveredAt();

        // Fallback for older orders created before deliveredAt existed.
        if (order.getStatus() == OrderStatus.DELIVERED) {
            return order.getLastModifiedDate() != null ? order.getLastModifiedDate() : order.getCreatedDate();
        }
        return null;
    }

    private Address requireAddress(User user, Integer addressId) {
        if (addressId == null) {
            throw new BadRequestException("Dia chi giao hang khong duoc trong");
        }
        return addressRepository.findByIdAndUser(addressId.longValue(), user)
                .orElseThrow(() -> new NotFoundException("Address not found"));
    }

    private String resolveRecipientName(User user) {
        if (user == null) return null;
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return user.getEmail();
    }

    private String resolveRecipientPhone(User user, Address address) {
        String fromAddress = address != null ? trimToNull(address.getPhoneNumber()) : null;
        if (fromAddress != null) return fromAddress;
        return user != null ? trimToNull(user.getPhoneNumber()) : null;
    }

    private String resolveOrderRecipient(Order order) {
        String fromSnapshot = trimToNull(order.getRecipientName());
        if (fromSnapshot != null) return fromSnapshot;
        // best effort fallback
        if (order.getUserId() != null) {
            return userRepository.findById(order.getUserId()).map(this::resolveRecipientName).orElse(null);
        }
        return null;
    }

    private String resolveOrderRecipientPhone(Order order) {
        String fromSnapshot = trimToNull(order.getRecipientPhone());
        if (fromSnapshot != null) return fromSnapshot;
        Address address = findAddressByOrder(order);
        if (address != null) {
            String fromAddress = trimToNull(address.getPhoneNumber());
            if (fromAddress != null) return fromAddress;
        }
        if (order.getUserId() != null) {
            return userRepository.findById(order.getUserId()).map(u -> trimToNull(u.getPhoneNumber())).orElse(null);
        }
        return null;
    }

    private String resolveOrderDeliveryAddress(Order order) {
        String fromSnapshot = trimToNull(order.getDeliveryAddress());
        Address address = findAddressByOrder(order);

        // Backward-compat: older snapshots were built as "street, city, state, ..." (province before district).
        // If we can still resolve the Address record and the snapshot matches that legacy ordering, rebuild it.
        if (fromSnapshot != null && address != null && isLegacyAddressSnapshot(fromSnapshot, address)) {
            return buildDeliveryAddress(address);
        }

        if (fromSnapshot != null) return fromSnapshot;
        if (address != null) return buildDeliveryAddress(address);
        return null;
    }

    private Address findAddressByOrder(Order order) {
        if (order == null || order.getAddressId() == null) return null;
        return addressRepository.findById(order.getAddressId().longValue()).orElse(null);
    }

    private String buildDeliveryAddress(Address address) {
        if (address == null) return null;
        String street = trimToNull(address.getStreet());
        String city = trimToNull(address.getCity());
        String state = trimToNull(address.getState());
        String country = trimToNull(address.getCountry());
        String zip = trimToNull(address.getZipCode());

        // Simple, readable format (avoid trailing commas).
        // NOTE: In this project, address.city is used as "Tinh/Thanh pho" and address.state as "Quan/Huyen".
        // So the Vietnamese display order should be: street, state (district), city (province), zip, country.
        StringBuilder sb = new StringBuilder();
        if (street != null) sb.append(street);
        if (state != null) appendWithComma(sb, state);
        if (city != null) appendWithComma(sb, city);
        if (zip != null) appendWithComma(sb, zip);
        if (country != null) appendWithComma(sb, country);
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void appendWithComma(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(part.trim());
    }

    private static boolean isLegacyAddressSnapshot(String snapshot, Address address) {
        if (snapshot == null || snapshot.isBlank() || address == null) return false;
        String street = trimToNull(address.getStreet());
        String city = trimToNull(address.getCity());
        String state = trimToNull(address.getState());
        if (street == null || city == null || state == null) return false;

        String[] parts = snapshot.split(",");
        if (parts.length < 3) return false;

        String p0 = parts[0].trim();
        String p1 = parts[1].trim();
        String p2 = parts[2].trim();

        return equalsIgnoreCaseTrim(p0, street)
                && equalsIgnoreCaseTrim(p1, city)
                && equalsIgnoreCaseTrim(p2, state);
    }

    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String coalesce(String a, String b) {
        return trimToNull(a) != null ? a : b;
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
