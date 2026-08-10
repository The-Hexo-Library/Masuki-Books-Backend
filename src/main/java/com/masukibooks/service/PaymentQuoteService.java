package com.masukibooks.service;

import com.masukibooks.dto.response.PaymentQuoteResponse;
import com.masukibooks.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQuoteService {

    private final OrderService orderService;
    private final ExchangeRateService exchangeRateService;

    public PaymentQuoteResponse quoteForUser(UUID userId, String gateway) {
        OrderService.CartSnapshot cartSnapshot = orderService.previewCart(userId, null);
        return quoteForAmount(cartSnapshot.getSubtotalUsd(), gateway);
    }

    public PaymentQuoteResponse quoteForAmount(BigDecimal usdAmount, String gateway) {
        if (usdAmount == null) {
            throw new IllegalArgumentException("usdAmount is required");
        }

        String resolvedGateway = normalizeGateway(gateway);
        if ("stripe".equals(resolvedGateway)) {
            BigDecimal amount = usdAmount.setScale(2, RoundingMode.HALF_UP);
            return PaymentQuoteResponse.builder()
                    .usdAmount(amount)
                    .exchangeRate(BigDecimal.ONE)
                    .currency("USD")
                    .convertedAmount(amount)
                    .amountPaise(toMinorUnits(amount))
                    .build();
        }

        if (!"razorpay".equals(resolvedGateway)) {
            throw new BusinessException("Unsupported payment gateway: " + gateway);
        }

        BigDecimal exchangeRate = exchangeRateService.getUsdToInrRate();
        BigDecimal convertedAmount = usdAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        return PaymentQuoteResponse.builder()
                .usdAmount(usdAmount.setScale(2, RoundingMode.HALF_UP))
                .exchangeRate(exchangeRate.setScale(2, RoundingMode.HALF_UP))
                .currency("INR")
                .convertedAmount(convertedAmount)
                .amountPaise(toMinorUnits(convertedAmount))
                .build();
    }

    public String resolveCurrency(String gateway) {
        String resolvedGateway = normalizeGateway(gateway);
        if ("stripe".equals(resolvedGateway)) {
            return "USD";
        }
        if ("razorpay".equals(resolvedGateway)) {
            return "INR";
        }
        throw new BusinessException("Unsupported payment gateway: " + gateway);
    }

    public BigDecimal resolveExchangeRate(String gateway) {
        String resolvedGateway = normalizeGateway(gateway);
        if ("stripe".equals(resolvedGateway)) {
            return BigDecimal.ONE;
        }
        if ("razorpay".equals(resolvedGateway)) {
            return exchangeRateService.getUsdToInrRate();
        }
        throw new BusinessException("Unsupported payment gateway: " + gateway);
    }

    private String normalizeGateway(String gateway) {
        return gateway == null ? "" : gateway.trim().toLowerCase(Locale.ROOT);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}