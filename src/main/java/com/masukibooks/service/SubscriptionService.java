package com.masukibooks.service;

import com.masukibooks.dto.request.SubscriptionPlanRequest;
import com.masukibooks.dto.response.SubscriptionResponse;
import com.masukibooks.dto.response.SubscriptionStatusResponse;
import com.masukibooks.dto.response.WalletResponse;
import com.masukibooks.entity.Subscription;
import com.masukibooks.entity.SubscriptionStatus;
import com.masukibooks.entity.User;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.PublicLibraryRepository;
import com.masukibooks.repository.SubscriptionRepository;
import com.masukibooks.repository.UserLibraryRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final PublicLibraryRepository publicLibraryRepository;

    public List<SubscriptionResponse> listAvailablePlans() {
        ensureDefaultPlans();
        return subscriptionRepository.findByIsPlanTrueAndIsActiveTrueOrderByPriceAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse createSubscriptionPlan(SubscriptionPlanRequest request) {
        return createPlan(request);
    }

    @Transactional
    public SubscriptionResponse subscribeUser(UUID userId, UUID planId) {
        return activatePlan(userId, planId);
    }

    public SubscriptionStatusResponse getUserSubscription(UUID userId) {
        Optional<Subscription> latest = subscriptionRepository.findFirstByUserUserIdAndIsPlanFalseOrderByCreatedAtDesc(userId);

        if (latest.isEmpty()) {
            long totalPublicBooks = publicLibraryRepository.countByVisibility("public");
            long usedBooks = userLibraryRepository.countByUserUserIdAndStatus(userId, "active");
            return SubscriptionStatusResponse.builder()
                    .active(false)
                    .planName("No Subscription")
                    .accessPercentage(0)
                    .status(SubscriptionStatus.EXPIRED)
                    .totalPublicBooks(totalPublicBooks)
                    .usedBooks(usedBooks)
                    .allowedBooks(0L)
                    .limitExceeded(true)
                    .build();
        }

        Subscription subscription = latest.get();
        refreshStatusIfExpired(subscription);

        AccessLimitResult limit = checkAccessLimit(userId);
        return SubscriptionStatusResponse.builder()
                .active(Boolean.TRUE.equals(subscription.getIsActive()) && subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .planName(subscription.getPlanName())
                .accessPercentage(limit.accessPercentage())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiresAt(subscription.getExpiresAt())
                .totalPublicBooks(limit.totalPublicBooks())
                .usedBooks(limit.usedBooks())
                .allowedBooks(limit.allowedBooks())
                .limitExceeded(limit.limitExceeded())
                .build();
    }

    public AccessLimitResult checkAccessLimit(UUID userId) {
        Optional<Subscription> latest = subscriptionRepository.findFirstByUserUserIdAndIsPlanFalseOrderByCreatedAtDesc(userId);
        long totalPublicBooks = publicLibraryRepository.countByVisibility("public");
        long usedBooks = userLibraryRepository.countByUserUserIdAndStatus(userId, "active");

        if (latest.isEmpty()) {
            return new AccessLimitResult(0, totalPublicBooks, usedBooks, 0L, true);
        }

        Subscription subscription = latest.get();
        refreshStatusIfExpired(subscription);

        int accessPercentage = getAccessPercentage(subscription.getPlanName(), subscription.getDescription());
        long allowedBooks = calculateAllowedBooks(totalPublicBooks, accessPercentage);

        boolean inactive = subscription.getStatus() != SubscriptionStatus.ACTIVE || !Boolean.TRUE.equals(subscription.getIsActive());
        boolean limitExceeded = inactive || usedBooks >= allowedBooks;

        return new AccessLimitResult(accessPercentage, totalPublicBooks, usedBooks, allowedBooks, limitExceeded);
    }

    public boolean isSubscriptionActive(UUID userId) {
        Optional<Subscription> latest = subscriptionRepository.findFirstByUserUserIdAndIsPlanFalseOrderByCreatedAtDesc(userId);
        if (latest.isEmpty()) {
            return false;
        }
        Subscription subscription = latest.get();
        refreshStatusIfExpired(subscription);
        return subscription.getStatus() == SubscriptionStatus.ACTIVE && Boolean.TRUE.equals(subscription.getIsActive());
    }

    @Transactional
    public SubscriptionResponse createPlan(SubscriptionPlanRequest request) {
        Subscription plan = Subscription.builder()
                .planName(request.getPlanName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .isPlan(true)
                .status(SubscriptionStatus.ACTIVE)
                .isActive(true)
                .autoRenew(Boolean.TRUE.equals(request.getAutoRenew()))
                .build();
        return toResponse(subscriptionRepository.save(plan));
    }

    @Transactional
    public SubscriptionResponse activatePlan(UUID userId, UUID planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Subscription plan = subscriptionRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(plan.getDurationDays());

        subscriptionRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(existing -> !Boolean.TRUE.equals(existing.getIsPlan()) && existing.getStatus() == SubscriptionStatus.ACTIVE)
                .forEach(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    existing.setIsActive(false);
                });

        Subscription activeSubscription = Subscription.builder()
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .isPlan(false)
                .user(user)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(start)
                .expiresAt(end)
                .autoRenew(plan.getAutoRenew())
                .isActive(true)
                .build();

        return toResponse(subscriptionRepository.save(activeSubscription));
    }

    /**
     * Purchase a wallet-credit plan: adds the plan's price to the user's wallet balance.
     * Users can purchase any plan any number of times.
     */
    @Transactional
    public WalletResponse purchaseWalletPlan(UUID userId, UUID planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Subscription plan = subscriptionRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        BigDecimal amount = plan.getPrice();
        BigDecimal currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        // Record the purchase as a subscription record for history tracking
        Subscription purchaseRecord = Subscription.builder()
                .planName(plan.getPlanName())
                .description("Wallet top-up: " + plan.getPlanName())
                .price(plan.getPrice())
                .durationDays(0)
                .isPlan(false)
                .user(user)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .autoRenew(false)
                .isActive(true)
                .build();
        subscriptionRepository.save(purchaseRecord);

        return WalletResponse.builder()
                .balance(newBalance)
                .amountAdded(amount)
                .message("Successfully added " + amount + " to wallet. New balance: " + newBalance)
                .build();
    }

    /**
     * Get the current wallet balance for a user.
     */
    public BigDecimal getWalletBalance(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
    }

    /**
     * Deduct an amount from the user's wallet. Returns the actual amount deducted.
     */
    @Transactional
    public BigDecimal deductFromWallet(UUID userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal deduction = amount.min(currentBalance);
        user.setWalletBalance(currentBalance.subtract(deduction));
        userRepository.save(user);
        return deduction;
    }

    public List<SubscriptionResponse> getUserSubscriptions(UUID userId) {
        return subscriptionRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .planName(subscription.getPlanName())
                .accessPercentage(getAccessPercentage(subscription.getPlanName(), subscription.getDescription()))
                .description(subscription.getDescription())
                .price(subscription.getPrice())
                .durationDays(subscription.getDurationDays())
                .isPlan(subscription.getIsPlan())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiresAt(subscription.getExpiresAt())
                .autoRenew(subscription.getAutoRenew())
                .build();
    }

    private void refreshStatusIfExpired(Subscription subscription) {
        if (subscription.getExpiresAt() != null
                && subscription.getExpiresAt().isBefore(LocalDateTime.now())
                && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setIsActive(false);
            subscriptionRepository.save(subscription);
        }
    }

    private void ensureDefaultPlans() {
        if (!subscriptionRepository.findByIsPlanTrueAndIsActiveTrueOrderByPriceAsc().isEmpty()) {
            return;
        }

        List<Subscription> defaults = List.of(
                Subscription.builder()
                        .planName("Starter $25")
                        .description("Add $25 to your wallet balance. Use it to purchase books during checkout.")
                        .price(BigDecimal.valueOf(25))
                        .durationDays(365)
                        .isPlan(true)
                        .status(SubscriptionStatus.ACTIVE)
                        .isActive(true)
                        .autoRenew(false)
                        .build(),
                Subscription.builder()
                        .planName("Plus $50")
                        .description("Add $50 to your wallet balance. Best value for regular readers.")
                        .price(BigDecimal.valueOf(50))
                        .durationDays(365)
                        .isPlan(true)
                        .status(SubscriptionStatus.ACTIVE)
                        .isActive(true)
                        .autoRenew(false)
                        .build(),
                Subscription.builder()
                        .planName("Premium $100")
                        .description("Add $100 to your wallet balance. Maximum savings for avid collectors.")
                        .price(BigDecimal.valueOf(100))
                        .durationDays(365)
                        .isPlan(true)
                        .status(SubscriptionStatus.ACTIVE)
                        .isActive(true)
                        .autoRenew(false)
                        .build()
        );

        subscriptionRepository.saveAll(defaults);
    }

    private long calculateAllowedBooks(long totalPublicBooks, int accessPercentage) {
        if (accessPercentage >= 100) {
            return totalPublicBooks;
        }
        long allowed = Math.round((totalPublicBooks * accessPercentage) / 100.0d);
        return Math.max(1, allowed);
    }

    private int getAccessPercentage(String planName, String description) {
        Pattern pattern = Pattern.compile("(\\d{1,3})\\s*%", Pattern.CASE_INSENSITIVE);
        Matcher planMatcher = pattern.matcher(planName == null ? "" : planName);
        if (planMatcher.find()) {
            return clampPercentage(Integer.parseInt(planMatcher.group(1)));
        }
        Matcher descMatcher = pattern.matcher(description == null ? "" : description);
        if (descMatcher.find()) {
            return clampPercentage(Integer.parseInt(descMatcher.group(1)));
        }

        String source = (planName == null ? "" : planName).toLowerCase();
        if (source.contains("full") || source.contains("premium") || source.contains("100")) {
            return 100;
        }
        return 10;
    }

    private int clampPercentage(int value) {
        return Math.max(1, Math.min(100, value));
    }

    public record AccessLimitResult(
            int accessPercentage,
            long totalPublicBooks,
            long usedBooks,
            long allowedBooks,
            boolean limitExceeded
    ) {
    }
}
