package com.masukibooks.controller;

import com.masukibooks.dto.request.CartItemRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.CartResponse;
import com.masukibooks.security.JwtTokenProvider;
import com.masukibooks.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtTokenProvider jwtTokenProvider;

    private UUID resolveUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String token = authHeader.replace("Bearer ", "");
            if (jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {
        UUID userId = resolveUserId(authHeader);
        CartResponse cart = userId != null
                ? cartService.getOrCreateCartForUser(userId)
                : cartService.getOrCreateGuestCart(guestToken != null ? guestToken : UUID.randomUUID().toString());
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody CartItemRequest request) {
        UUID userId = resolveUserId(authHeader);
        CartResponse cart = userId != null
                ? cartService.getOrCreateCartForUser(userId)
                : cartService.getOrCreateGuestCart(guestToken);
        CartResponse updated = cartService.addItem(cart.getCartId(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", updated));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID cartItemId,
            @RequestBody Map<String, Integer> body) {
        UUID userId = resolveUserId(authHeader);
        CartResponse cart = userId != null
                ? cartService.getOrCreateCartForUser(userId)
                : cartService.getOrCreateGuestCart(guestToken);
        CartResponse updated = cartService.updateItem(cart.getCartId(), cartItemId, body.get("quantity"));
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", updated));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID cartItemId) {
        UUID userId = resolveUserId(authHeader);
        CartResponse cart = userId != null
                ? cartService.getOrCreateCartForUser(userId)
                : cartService.getOrCreateGuestCart(guestToken);
        CartResponse updated = cartService.removeItem(cart.getCartId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", updated));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {
        UUID userId = resolveUserId(authHeader);
        CartResponse cart = userId != null
                ? cartService.getOrCreateCartForUser(userId)
                : cartService.getOrCreateGuestCart(guestToken);
        cartService.clearCart(cart.getCartId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
