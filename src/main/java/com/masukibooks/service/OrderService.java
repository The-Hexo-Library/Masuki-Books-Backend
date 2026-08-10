package com.masukibooks.service;

import com.masukibooks.dto.request.CheckoutRequest;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.entity.BooksMetadata;
import com.masukibooks.entity.Cart;
import com.masukibooks.entity.CartItem;
import com.masukibooks.entity.Order;
import com.masukibooks.entity.OrderItem;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.CartRepository;
import com.masukibooks.repository.OrderItemRepository;
import com.masukibooks.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    public static class CartSnapshot {
        private final Cart cart;
        private final List<CartItem> items;
        private final BigDecimal subtotalUsd;

        public CartSnapshot(Cart cart, List<CartItem> items, BigDecimal subtotalUsd) {
            this.cart = cart;
            this.items = items;
            this.subtotalUsd = subtotalUsd;
        }

        public Cart getCart() {
            return cart;
        }

        public List<CartItem> getItems() {
            return items;
        }

        public BigDecimal getSubtotalUsd() {
            return subtotalUsd;
        }
    }

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderResponse checkout(UUID userId, String guestToken, CheckoutRequest request, String resolvedCurrency,
            BigDecimal exchangeRate) {
        CartSnapshot snapshot = previewCart(userId, guestToken);
        Cart cart = snapshot.getCart();
        List<CartItem> items = snapshot.getItems();
        BigDecimal subtotalUsd = snapshot.getSubtotalUsd();
        String currency = (resolvedCurrency == null || resolvedCurrency.isBlank()) ? "USD" : resolvedCurrency.trim()
                .toUpperCase();
        BigDecimal effectiveExchangeRate = exchangeRate == null ? BigDecimal.ONE : exchangeRate;

        if (!"USD".equals(currency) && effectiveExchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A positive exchange rate is required for non-USD checkout.");
        }

        BigDecimal subtotal = convertAmount(subtotalUsd, currency, effectiveExchangeRate);
        boolean allDigital = true;
        boolean hasDigital = false;

        for (CartItem item : items) {
            BooksMetadata product = item.getProduct();
            boolean isDigital = "digital".equals(product.getContentType()) || "both".equals(product.getContentType());

            if (isDigital) {
                hasDigital = true;
            } else {
                allDigital = false;
            }
        }

        String orderType = allDigital ? "digital" : (hasDigital ? "mixed" : "physical");

        BigDecimal discountAmount = BigDecimal.ZERO;

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
                .currency(currency)
                .status("pending")
                .build();

        order = orderRepository.save(order);
        final UUID orderId = order.getOrderId();

        for (CartItem item : items) {
            BigDecimal itemUnitPrice = convertAmount(item.getUnitPrice(), currency, effectiveExchangeRate);
            BigDecimal itemTotalPrice = convertAmount(
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())), currency,
                effectiveExchangeRate);
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(item.getProduct())
                    .quantity(item.getQuantity())
                .unitPrice(itemUnitPrice)
                .totalPrice(itemTotalPrice)
                    .productTitle(item.getProduct().getTitle())
                    .build();
            orderItemRepository.save(orderItem);

        }

        cart.setStatus("converted");
        cartRepository.save(cart);

        return toResponse(orderRepository.findById(orderId).get());
    }

    public CartSnapshot previewCart(UUID userId, String guestToken) {
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

        BigDecimal subtotalUsd = BigDecimal.ZERO;
        for (CartItem item : items) {
            subtotalUsd = subtotalUsd.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        return new CartSnapshot(cart, items, subtotalUsd);
    }

    private BigDecimal convertAmount(BigDecimal amountUsd, String currency, BigDecimal exchangeRate) {
        if (amountUsd == null) {
            return BigDecimal.ZERO;
        }
        if (currency == null || currency.isBlank() || "USD".equalsIgnoreCase(currency)) {
            return amountUsd.setScale(2, RoundingMode.HALF_UP);
        }
        return amountUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
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
            .userId(o.getUser() != null ? o.getUser().getUserId() : null)
            .userEmail(o.getUser() != null ? o.getUser().getEmail() : null)
            .userFirstName(o.getUser() != null ? o.getUser().getFirstName() : null)
            .userLastName(o.getUser() != null ? o.getUser().getLastName() : null)
            .userName(o.getUser() != null
                ? String.format("%s %s",
                    o.getUser().getFirstName() != null ? o.getUser().getFirstName() : "",
                    o.getUser().getLastName() != null ? o.getUser().getLastName() : "").trim()
                : null)
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
