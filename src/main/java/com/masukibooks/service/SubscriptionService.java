package com.masukibooks.service;

import com.masukibooks.dto.request.SubscriptionPlanRequest;
import com.masukibooks.dto.response.SubscriptionResponse;
import com.masukibooks.entity.Subscription;
import com.masukibooks.entity.SubscriptionStatus;
import com.masukibooks.entity.User;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.SubscriptionRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public List<SubscriptionResponse> listAvailablePlans() {
        return subscriptionRepository.findByIsPlanTrueAndIsActiveTrueOrderByPriceAsc()
                .stream()
                .map(this::toResponse)
                .toList();
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
}
