package com.masukibooks.controller;

import com.masukibooks.dto.request.ActivateSubscriptionRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.SubscriptionResponse;
import com.masukibooks.dto.response.SubscriptionStatusResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success("Subscription plans retrieved", subscriptionService.listAvailablePlans()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ActivateSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription activated",
                subscriptionService.subscribeUser(user.getUserId(), request.getSubscriptionPlanId())
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getMySubscription(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription status retrieved",
                subscriptionService.getUserSubscription(user.getUserId())
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription status retrieved",
                subscriptionService.getUserSubscription(user.getUserId())
        ));
    }
}
