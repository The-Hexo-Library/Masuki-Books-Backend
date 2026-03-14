package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class CartResponse {
    private UUID cartId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private int totalItems;

    @Data @Builder
    public static class CartItemResponse {
        private UUID cartItemId;
        private UUID productId;
        private String productTitle;
        private String productImageUrl;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private boolean inStock;
    }
}
