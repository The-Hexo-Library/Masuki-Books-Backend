package com.masukibooks.service;

import com.masukibooks.dto.request.RazorpayVerifyPaymentRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentService {

    private final OrderService orderService;
    private final PaymentService paymentService;

    private final RestClient restClient = RestClient.create();

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    /** Exposes the public Razorpay key id so the frontend can open the checkout widget. */
    public String getKeyId() {
        return keyId;
    }

    /** Default currency used when a request does not specify one. */
    public String getDefaultCurrency() {
        return (defaultCurrency == null || defaultCurrency.isBlank()) ? "INR" : defaultCurrency;
    }

    /**
     * Creates a Razorpay order for the given internal order.
     * This method is used by POST /api/create-order.
     */
    public Map<String, Object> createOrder(long amountPaise, String currency, String receipt, String internalOrderId) {
        if (amountPaise < 100) {
            throw new IllegalArgumentException("Minimum amount is 100 paise.");
        }
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("Razorpay key id not configured.");
        }
        String resolvedCurrency = (currency == null || currency.isBlank()) ? getDefaultCurrency() : currency;
        if (receipt == null || receipt.isBlank()) {
            throw new IllegalArgumentException("receipt is required.");
        }

        String basicAuth = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        try {
            // Razorpay expects: amount (paise), currency, receipt
            Map<String, Object> payload = Map.of(
                    "amount", amountPaise,
                    "currency", resolvedCurrency,
                    "receipt", receipt);

            return restClient.post()
                    .uri("https://api.razorpay.com/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            // Razorpay responded with an HTTP error (e.g. 401 auth failure, 400 bad amount/currency).
            String body = ex.getResponseBodyAsString();
            log.error("Razorpay order creation rejected: status={} body={}", ex.getStatusCode(), body);
            if (ex.getStatusCode().value() == 401) {
                // Surfaced as 400 (not 401) so the SPA does not treat it as an expired session.
                throw new IllegalArgumentException(
                        "Razorpay authentication failed. Verify RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET are valid Razorpay keys from the same keypair.");
            }
            throw new IllegalArgumentException("Razorpay rejected the order. " + body);
        } catch (RestClientException ex) {
            log.error("Could not reach Razorpay", ex);
            throw new RuntimeException("Could not reach Razorpay. Check the server's network connection.", ex);
        }
    }

    /**
     * Verifies Razorpay signature and marks internal order payment success only if
     * it matches.
     * Used by POST /api/verify-payment.
     */
    @Transactional
    public void verifyAndCompletePayment(RazorpayVerifyPaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String razorpayOrderId = request.getRazorpayOrderId();
        String razorpayPaymentId = request.getRazorpayPaymentId();
        String razorpaySignature = request.getRazorpaySignature();
        String internalOrderId = request.getInternalOrderId();

        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()
                || razorpaySignature == null || razorpaySignature.isBlank()
                || internalOrderId == null || internalOrderId.isBlank()) {
            throw new IllegalArgumentException("Missing required fields");
        }

        if (keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay key secret not configured.");
        }

        String data = razorpayOrderId + "|" + razorpayPaymentId;
        String expectedSignature = hmacSha256Hex(data, keySecret);

        if (!constantTimeEquals(expectedSignature, razorpaySignature)) {
            throw new SignatureMismatchException("Signature mismatch");
        }

        // Mark payment success/unlock content.
        // We treat razorpayPaymentId as the gatewayTransactionId.
        // PaymentService.markPaymentSuccess expects internal orderId(UUID) +
        // gatewayTransactionId.
        // internalOrderId is the app's Order UUID created during /user/checkout
        paymentService.markPaymentSuccess(java.util.UUID.fromString(internalOrderId), razorpayPaymentId);
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC SHA256", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null)
            return false;
        if (a.length() != b.length())
            return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public static class SignatureMismatchException extends RuntimeException {
        public SignatureMismatchException(String message) {
            super(message);
        }
    }
}
