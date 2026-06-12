package com.masukibooks.service;

import com.masukibooks.config.RazorpayProperties;
import com.masukibooks.entity.Order;
import com.masukibooks.entity.Payment;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final RazorpayProperties razorpayProperties;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public record RazorpayCheckoutDetails(
            String razorpayOrderId,
            long amount,
            String currency,
            String keyId
    ) {}

    private RazorpayClient client() throws RazorpayException {
        if (razorpayProperties.getKeyId() == null || razorpayProperties.getKeyId().isBlank()
                || razorpayProperties.getKeySecret() == null || razorpayProperties.getKeySecret().isBlank()) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
    }

    @Transactional
    public RazorpayCheckoutDetails createOrderForCheckout(Order order) throws RazorpayException {
        String currency = normalizeCurrency(order.getCurrency());
        long amount = toSmallestCurrencyUnit(order.getTotalAmount(), currency);

        JSONObject notes = new JSONObject();
        notes.put("order_id", order.getOrderId().toString());
        notes.put("order_number", order.getOrderNumber());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", order.getOrderNumber());
        orderRequest.put("notes", notes);

        com.razorpay.Order razorpayOrder = client().orders.create(orderRequest);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("card")
                .gateway("razorpay")
                .amount(order.getTotalAmount())
                .currency(currency)
                .status("pending")
                .gatewayTransactionId(razorpayOrder.get("id"))
                .build();
        paymentRepository.save(payment);

        return new RazorpayCheckoutDetails(
                razorpayOrder.get("id"),
                amount,
                currency,
                razorpayProperties.getKeyId()
        );
    }

    @Transactional
    public void verifyAndCompletePayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", razorpayPaymentId);
        options.put("razorpay_signature", razorpaySignature);

        try {
            if (!Utils.verifyPaymentSignature(options, razorpayProperties.getKeySecret())) {
                throw new BusinessException("Invalid Razorpay payment signature");
            }
        } catch (RazorpayException ex) {
            throw new BusinessException("Payment verification failed: " + ex.getMessage());
        }

        Payment payment = paymentRepository.findByGatewayTransactionId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Razorpay order"));

        if ("success".equalsIgnoreCase(payment.getStatus())) {
            return;
        }

        payment.setStatus("success");
        payment.setGatewayPaymentId(razorpayPaymentId);
        paymentRepository.save(payment);

        paymentService.markPaymentSuccess(payment.getOrder().getOrderId(), razorpayPaymentId);
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (razorpayProperties.getWebhookSecret() == null || razorpayProperties.getWebhookSecret().isBlank()) {
            return true;
        }
        try {
            return Utils.verifyWebhookSignature(payload, signature, razorpayProperties.getWebhookSecret());
        } catch (RazorpayException ex) {
            return false;
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return razorpayProperties.getDefaultCurrency().toUpperCase();
        }
        return currency.trim().toUpperCase();
    }

    private long toSmallestCurrencyUnit(BigDecimal amount, String currency) {
        BigDecimal total = amount == null ? BigDecimal.ZERO : amount;
        return total.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
