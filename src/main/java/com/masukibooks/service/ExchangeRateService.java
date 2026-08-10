package com.masukibooks.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class ExchangeRateService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RestClient restClient;
    private final AtomicReference<CachedRate> cachedUsdToInrRate = new AtomicReference<>();

    @Value("${exchange-rate.usd-inr-url:https://api.frankfurter.app/latest?from=USD&to=INR}")
    private String usdToInrUrl;

    @Value("${exchange-rate.usd-inr-fallback-url:https://open.er-api.com/v6/latest/USD}")
    private String usdToInrFallbackUrl;

    public ExchangeRateService() {
        this.restClient = RestClient.builder().defaultHeader(HttpHeaders.ACCEPT, "application/json").build();
    }

    public BigDecimal getUsdToInrRate() {
        CachedRate cached = cachedUsdToInrRate.get();
        if (cached != null && !cached.isExpired()) {
            return cached.rate();
        }

        try {
            BigDecimal fetchedRate = fetchUsdToInrRate();
            cachedUsdToInrRate.set(new CachedRate(fetchedRate, Instant.now()));
            return fetchedRate;
        } catch (RuntimeException ex) {
            if (cached != null) {
                log.warn("USD/INR rate fetch failed; falling back to cached rate {} from {}", cached.rate(), cached.fetchedAt(), ex);
                return cached.rate();
            }
            throw ex;
        }
    }

    public BigDecimal convertUsdToInr(BigDecimal usdAmount) {
        if (usdAmount == null) {
            throw new IllegalArgumentException("usdAmount is required");
        }
        BigDecimal rate = getUsdToInrRate();
        return usdAmount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal fetchUsdToInrRate() {
        RuntimeException primaryFailure = null;
        try {
            return fetchRateFromFrankfurter();
        } catch (RuntimeException ex) {
            primaryFailure = ex;
            log.warn("Primary USD/INR provider failed; trying fallback provider", ex);
        }

        try {
            return fetchRateFromOpenErApi();
        } catch (RuntimeException fallbackFailure) {
            if (primaryFailure != null) {
                fallbackFailure.addSuppressed(primaryFailure);
            }
            throw new IllegalStateException("Unable to fetch USD to INR exchange rate from live providers", fallbackFailure);
        }
    }

    private BigDecimal fetchRateFromFrankfurter() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(usdToInrUrl)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Empty exchange-rate response from Frankfurter");
            }

            Object ratesObj = response.get("rates");
            if (!(ratesObj instanceof Map<?, ?> rates)) {
                throw new IllegalStateException("Exchange-rate response missing rates");
            }

            Object inrRate = rates.get("INR");
            if (inrRate == null) {
                throw new IllegalStateException("Exchange-rate response missing INR rate");
            }

            return validateRate(new BigDecimal(String.valueOf(inrRate)));
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to fetch USD to INR exchange rate from Frankfurter", ex);
        }
    }

    private BigDecimal fetchRateFromOpenErApi() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(usdToInrFallbackUrl)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Empty exchange-rate response from OpenER API");
            }

            Object ratesObj = response.get("rates");
            if (!(ratesObj instanceof Map<?, ?> rates)) {
                throw new IllegalStateException("Exchange-rate response missing rates");
            }

            Object inrRate = rates.get("INR");
            if (inrRate == null) {
                throw new IllegalStateException("Exchange-rate response missing INR rate");
            }

            return validateRate(new BigDecimal(String.valueOf(inrRate)));
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to fetch USD to INR exchange rate from OpenER API", ex);
        }
    }

    private BigDecimal validateRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Exchange-rate response returned a non-positive INR rate");
        }
        return rate;
    }

    private record CachedRate(BigDecimal rate, Instant fetchedAt) {
        boolean isExpired() {
            return fetchedAt.plus(CACHE_TTL).isBefore(Instant.now());
        }
    }
}