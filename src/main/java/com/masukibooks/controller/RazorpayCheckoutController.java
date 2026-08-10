package com.masukibooks.controller;

import com.masukibooks.dto.request.RazorpayCreateOrderRequest;
import com.masukibooks.dto.request.RazorpayVerifyPaymentRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RazorpayCheckoutController {

    private final RazorpayPaymentService razorpayPaymentService;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @Valid @RequestBody RazorpayCreateOrderRequest request) {

        try {
            long amountPaise = request.getAmount();
            Map<String, Object> razorpayOrder = razorpayPaymentService.createOrder(
                    amountPaise,
                    request.getReceipt(),
                    request.getInternalOrderId());

            Object orderId = razorpayOrder.get("id");

            return ResponseEntity.ok(ApiResponse.success("Order created", Map.of(
                    "order_id", orderId,
                    "amount", razorpayOrder.get("amount"),
                    "currency", razorpayOrder.get("currency"),
                    "key_id", razorpayPaymentService.getKeyId())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Map<String, Object>>error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>error("Failed to create Razorpay order"));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<ApiResponse<Void>> verifyPayment(
            @Valid @RequestBody RazorpayVerifyPaymentRequest request) {

        try {
            razorpayPaymentService.verifyAndCompletePayment(request);
            return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>error(ex.getMessage()));
        } catch (RazorpayPaymentService.SignatureMismatchException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>error("Signature mismatch"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>error("Failed to verify payment"));
        }
    }
}
