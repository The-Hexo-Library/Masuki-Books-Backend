package com.masukibooks.service;

import com.masukibooks.dto.request.CheckoutRequest;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    @SuppressWarnings("unused")
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    // private final AddressRepository addressRepository;
    private final DiscountCodeRepository discountCodeRepository;
    @SuppressWarnings("unused")
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderResponse checkout(UUID userId, String guestToken, CheckoutRequest request) {
        Cart cart;
        if (userId != null) {
            cart = cartRepository.findByUserUserIdAndStatus(userId, "active")
                    .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));
        } else {
            cart = cartRepository.findByGuestTokenAndStatus(guestToken, "active")
                    .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));
        }

        List<CartItem> items = cart.getItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allDigital = true;
        boolean hasDigital = false;

        for (CartItem item : items) {
            Product product = item.getProduct();
            boolean isDigital = "digital".equals(product.getContentType()) || "both".equals(product.getContentType());

            if (isDigital) {
                hasDigital = true;
            } else {
                allDigital = false;
            }

            // Only check inventory for physical products
            if (!isDigital) {
                Inventory inv = inventoryRepository.findByProductProductId(product.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Inventory not found for: " + product.getTitle()));
                if (inv.getQuantity() < item.getQuantity()) {
                    throw new BusinessException("Insufficient stock for: " + product.getTitle());
                }
            }
            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        String orderType = allDigital ? "digital" : (hasDigital ? "mixed" : "physical");

        BigDecimal discountAmount = BigDecimal.ZERO;
        DiscountCode discountCode = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            discountCode = discountCodeRepository.findByCodeAndIsActiveTrue(request.getDiscountCode())
                    .orElseThrow(() -> new BusinessException("Invalid discount code"));
            if (discountCode.getExpiresAt() != null && discountCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("Discount code has expired");
            }
            if (discountCode.getMaxUses() != null && discountCode.getUsedCount() >= discountCode.getMaxUses()) {
                throw new BusinessException("Discount code usage limit reached");
            }
            if (discountCode.getMinOrderAmount() != null
                    && subtotal.compareTo(discountCode.getMinOrderAmount()) < 0) {
                throw new BusinessException("Order does not meet minimum amount for discount");
            }
            if ("percentage".equals(discountCode.getType())) {
                discountAmount = subtotal.multiply(discountCode.getValue())
                        .divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = discountCode.getValue().min(subtotal);
            }
        }

        BigDecimal total = subtotal.subtract(discountAmount);

        // Address handling: required for physical/mixed, optional for digital
        // Address shippingAddress = null;
        // Address billingAddress = null;

        // if (!"digital".equals(orderType)) {
        // if (request.getShippingAddressId() == null) {
        // throw new BusinessException("Shipping address is required for physical
        // orders");
        // }
        // shippingAddress = addressRepository.findById(request.getShippingAddressId())
        // .orElseThrow(() -> new ResourceNotFoundException("Shipping address not
        // found"));
        // billingAddress = request.getBillingAddressId() != null
        // ? addressRepository.findById(request.getBillingAddressId())
        // .orElseThrow(() -> new ResourceNotFoundException("Billing address not
        // found"))
        // : shippingAddress;
        // } else if (request.getShippingAddressId() != null) {
        // // Allow optional address for digital orders too
        // shippingAddress =
        // addressRepository.findById(request.getShippingAddressId()).orElse(null);
        // billingAddress = request.getBillingAddressId() != null
        // ? addressRepository.findById(request.getBillingAddressId()).orElse(null)
        // : shippingAddress;
        // }

        String orderNumber = "ORD-" + System.currentTimeMillis();

        Order order = Order.builder()
                .user(cart.getUser())
                .guestEmail(request.getGuestEmail())
                .orderNumber(orderNumber)
                .orderType(orderType)
                // .shippingAddress(shippingAddress)
                // .billingAddress(billingAddress)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                // .shippingAmount(shippingAmount)
                .totalAmount(total)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status("pending")
                .discountCode(discountCode)
                .build();

        order = orderRepository.save(order);
        final UUID orderId = order.getOrderId();

        for (CartItem item : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(item.getProduct())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .productTitle(item.getProduct().getTitle())
                    .build();
            orderItemRepository.save(orderItem);

            // Only deduct inventory for physical products
            Product product = item.getProduct();
            boolean isDigital = "digital".equals(product.getContentType()) || "both".equals(product.getContentType());
            if (!isDigital) {
                Inventory inv = inventoryRepository
                        .findByProductProductId(product.getProductId()).get();
                inv.setQuantity(inv.getQuantity() - item.getQuantity());
                inventoryRepository.save(inv);
            }
        }

        cart.setStatus("converted");
        cartRepository.save(cart);

        return toResponse(orderRepository.findById(orderId).get());
    }

    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserUserId(userId, pageable).map(this::toResponse);
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (userId != null && (order.getUser() == null
                || !order.getUser().getUserId().equals(userId))) {
            throw new BusinessException("Order does not belong to this user");
        }
        if (!List.of("pending", "confirmed").contains(order.getStatus())) {
            throw new BusinessException("Order cannot be cancelled in status: " + order.getStatus());
        }
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                inventoryRepository.findByProductProductId(item.getProduct().getProductId())
                        .ifPresent(inv -> {
                            inv.setQuantity(inv.getQuantity() + item.getQuantity());
                            inventoryRepository.save(inv);
                        });
            }
        }
        order.setStatus("cancelled");
        return toResponse(orderRepository.save(order));
    }

    public Page<OrderResponse> getAllOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return orderRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderResponse.OrderItemResponse> itemResponses = o.getItems() == null ? List.of()
                : o.getItems().stream().map(i -> OrderResponse.OrderItemResponse.builder()
                        .orderItemId(i.getOrderItemId())
                        .productId(i.getProduct().getProductId())
                        .productTitle(i.getProductTitle() != null
                                ? i.getProductTitle()
                                : i.getProduct().getTitle())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getTotalPrice())
                        .build()).collect(Collectors.toList());

        // OrderResponse.AddressResponse shipping = o.getShippingAddress() == null ?
        // null
        // : OrderResponse.AddressResponse.builder()
        // .addressId(o.getShippingAddress().getAddressId())
        // .addressLine1(o.getShippingAddress().getAddressLine1())
        // .city(o.getShippingAddress().getCity())
        // .state(o.getShippingAddress().getState())
        // .zipCode(o.getShippingAddress().getZipCode())
        // .country(o.getShippingAddress().getCountry())
        // .build();

        return OrderResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .orderType(o.getOrderType())
                .subtotal(o.getSubtotal())
                .discountAmount(o.getDiscountAmount())
                .totalAmount(o.getTotalAmount())
                .currency(o.getCurrency())
                .guestEmail(o.getGuestEmail())
                // .shippingAddress(shipping)
                .items(itemResponses)
                .createdAt(o.getCreatedAt())
                .build();
    }
}
