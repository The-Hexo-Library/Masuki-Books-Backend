package com.masukibooks.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayProperties {

    @Value("${app.razorpay.key-id:${RAZORPAY_KEY_ID:}}")
    private String keyId;

    @Value("${app.razorpay.key-secret:${RAZORPAY_KEY_SECRET:}}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:${RAZORPAY_WEBHOOK_SECRET:}}")
    private String webhookSecret;

    @Value("${app.razorpay.default-currency:INR}")
    private String defaultCurrency;

    public String getKeyId() {
        return keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }
}
