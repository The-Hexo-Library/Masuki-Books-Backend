package com.masukibooks.controller;

import com.masukibooks.dto.request.PaymentCallbackRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.entity.Payment;
import com.masukibooks.security.JwtTokenProvider;
import com.masukibooks.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<Payment>> initiate(@RequestBody Map<String, String> body) {
        UUID orderId = UUID.fromString(body.get("orderId"));
        Payment payment = paymentService.initiatePayment(orderId, body.get("gateway"), body.get("method"));
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", payment));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Payment>> callback(@Valid @RequestBody PaymentCallbackRequest request) {
        Payment payment = paymentService.handleCallback(request);
        return ResponseEntity.ok(ApiResponse.success("Payment callback processed", payment));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('user','admin','superadmin')")
    public ResponseEntity<ApiResponse<Payment>> getByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", paymentService.getPaymentByOrder(orderId)));
    }
}
