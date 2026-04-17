package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class OrderResponse {
    private UUID orderId;
    private String orderNumber;
    private String status;
    private String orderType;
    private UUID userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userName;
    private String guestEmail;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemResponse> items;
    private AddressResponse shippingAddress;
    private AddressResponse billingAddress;
    private String notes;
    private LocalDateTime createdAt;

    @Data @Builder
    public static class OrderItemResponse {
        private UUID orderItemId;
        private UUID productId;
        private String productTitle;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data @Builder
    public static class AddressResponse {
        private UUID addressId;
        private String fullName;
        private String addressLine1;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String phoneNumber;
    }
}
