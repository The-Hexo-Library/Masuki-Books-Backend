package com.masukibooks.controller;

import com.masukibooks.dto.request.RazorpayVerifyRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayPaymentService razorpayPaymentService;
    private final com.masukibooks.repository.OrderRepository orderRepository;
    private final com.masukibooks.repository.PaymentRepository paymentRepository;
    private final com.masukibooks.repository.ProcessedStripeEventRepository processedStripeEventRepository;
    private final com.masukibooks.service.PaymentService paymentService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RazorpayWebhookController.class);

    @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (!razorpayPaymentService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Razorpay webhook signature");
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            org.json.JSONObject event = new org.json.JSONObject(payload);
            String eventId = event.optString("id", payload.hashCode() + "-" + System.currentTimeMillis());

            if (processedStripeEventRepository.findByEventId(eventId).isPresent()) {
                return ResponseEntity.ok("skipped");
            }

            String eventType = event.optString("event", "");
            org.json.JSONObject payloadObj = event.optJSONObject("payload");
            if (payloadObj != null && ("payment.captured".equals(eventType) || "payment.authorized".equals(eventType))) {
                org.json.JSONObject paymentEntity = payloadObj.optJSONObject("payment");
                if (paymentEntity != null) {
                    org.json.JSONObject entity = paymentEntity.optJSONObject("entity");
                    if (entity != null) {
                        handlePaymentSuccess(
                                entity.optString("order_id", null),
                                entity.optString("id", null),
                                entity.optJSONObject("notes")
                        );
                    }
                }
            } else if ("payment.failed".equals(eventType) && payloadObj != null) {
                org.json.JSONObject paymentEntity = payloadObj.optJSONObject("payment");
                if (paymentEntity != null) {
                    org.json.JSONObject entity = paymentEntity.optJSONObject("entity");
                    if (entity != null) {
                        handlePaymentFailed(entity.optString("order_id", null), entity.optString("error_description", "Payment failed"));
                    }
                }
            }

            processedStripeEventRepository.save(com.masukibooks.entity.ProcessedStripeEvent.builder()
                    .eventId(eventId)
                    .processedAt(java.time.LocalDateTime.now())
                    .payload(payload)
                    .build());

            return ResponseEntity.ok("received");
        } catch (Exception ex) {
            log.error("Error processing Razorpay webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }

    private void handlePaymentSuccess(String razorpayOrderId, String razorpayPaymentId, org.json.JSONObject notes) {
        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            return;
        }

        paymentRepository.findByGatewayTransactionId(razorpayOrderId).ifPresent(payment -> {
            if ("success".equalsIgnoreCase(payment.getStatus())) {
                return;
            }
            payment.setStatus("success");
            payment.setGatewayPaymentId(razorpayPaymentId);
            paymentRepository.save(payment);

            com.masukibooks.entity.Order order = payment.getOrder();
            if (order != null) {
                order.setPaymentStatus("paid");
                order.setStatus("confirmed");
                order.setPaidAt(java.time.LocalDateTime.now());
                orderRepository.save(order);
                paymentService.markPaymentSuccess(order.getOrderId(), razorpayPaymentId);
            }
        });
    }

    private void handlePaymentFailed(String razorpayOrderId, String reason) {
        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            return;
        }
        paymentRepository.findByGatewayTransactionId(razorpayOrderId).ifPresent(payment -> {
            payment.setStatus("failed");
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
            if (payment.getOrder() != null) {
                payment.getOrder().setPaymentStatus("failed");
                orderRepository.save(payment.getOrder());
            }
        });
    }
}
