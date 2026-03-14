package com.masukibooks.controller;

import com.masukibooks.dto.request.CheckoutRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.security.JwtTokenProvider;
import com.masukibooks.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;

    private UUID getCurrentUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.replace("Bearer ", "");
        try {
            if (jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody CheckoutRequest request) {
        UUID userId = getCurrentUserId(authHeader);
        OrderResponse order = orderService.checkout(userId, guestToken, request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestHeader("Authorization") String authHeader,
            @PageableDefault(size = 10) Pageable pageable) {
        UUID userId = getCurrentUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orderService.getUserOrders(userId, pageable)));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", orderService.getOrder(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID orderId) {
        UUID userId = getCurrentUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(orderId, userId)));
    }
}
