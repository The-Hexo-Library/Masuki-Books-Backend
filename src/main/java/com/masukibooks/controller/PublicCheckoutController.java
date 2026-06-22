package com.masukibooks.controller;

import com.masukibooks.dto.request.UserCheckoutRequest;
import com.masukibooks.dto.request.CartItemRequest;
import com.masukibooks.dto.response.CheckoutFlowResponse;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.service.CheckoutFlowService;
import com.masukibooks.service.CartService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicCheckoutController {

    private final CheckoutFlowService checkoutFlowService;
    private final CartService cartService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutFlowResponse>> checkoutPublic(@RequestBody PublicCheckoutRequest request) {
        // Create a guest cart and populate items
        String guestToken = java.util.UUID.randomUUID().toString();
        var cart = cartService.getOrCreateGuestCart(guestToken);
        for (CartItemRequest it : request.getItems()) {
            cartService.addItem(cart.getCartId(), it);
        }

        UserCheckoutRequest uc = new UserCheckoutRequest();
        uc.setCurrency(request.getCurrency());
        uc.setDiscountCode(request.getDiscountCode());
        uc.setGateway(request.getGateway());
        uc.setPaymentMethod(request.getPaymentMethod());

        CheckoutFlowResponse resp = checkoutFlowService.checkoutAndInitiate(null, guestToken, uc);
        return ResponseEntity.ok(ApiResponse.success("Checkout session created", resp));
    }

    @Data
    public static class PublicCheckoutRequest {
        private java.util.List<CartItemRequest> items;
        private String currency;
        private String discountCode;
        private String gateway;
        private String paymentMethod;
    }
}
