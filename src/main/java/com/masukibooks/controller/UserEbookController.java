package com.masukibooks.controller;

import com.masukibooks.dto.request.*;
import com.masukibooks.dto.response.*;
import com.masukibooks.entity.User;
import com.masukibooks.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserEbookController {

    private final CategoryService categoryService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CheckoutFlowService checkoutFlowService;
    private final SubscriptionService subscriptionService;
    private final PublicLibraryService publicLibraryService;
    private final UserLibraryService userLibraryService;

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories() {
        List<CategoryResponse> dtos = categoryService.getRootCategories().stream().map(c -> CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .displayOrder(c.getDisplayOrder())
                .build()).toList();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", dtos));
    }

    @GetMapping("/cart")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", cartService.getOrCreateCartForUser(user.getUserId())));
    }

    @PostMapping("/cart/items")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> addCartItem(@AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequest request) {
        UUID cartId = cartService.getOrCreateCartForUser(user.getUserId()).getCartId();
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addItem(cartId, request)));
    }

    @PutMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(@AuthenticationPrincipal User user,
            @PathVariable UUID cartItemId,
            @RequestBody Map<String, Integer> request) {
        UUID cartId = cartService.getOrCreateCartForUser(user.getUserId()).getCartId();
        return ResponseEntity.ok(ApiResponse.success("Item updated", cartService.updateItem(cartId, cartItemId, request.get("quantity"))));
    }

    @DeleteMapping("/cart/items/{cartItemId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@AuthenticationPrincipal User user,
            @PathVariable UUID cartItemId) {
        UUID cartId = cartService.getOrCreateCartForUser(user.getUserId()).getCartId();
        return ResponseEntity.ok(ApiResponse.success("Item removed", cartService.removeItem(cartId, cartItemId)));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> myOrders(@AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orderService.getUserOrders(user.getUserId(), pageable)));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<CheckoutFlowResponse>> checkout(@AuthenticationPrincipal User user,
            @Valid @RequestBody UserCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Checkout completed and payment initiated",
                checkoutFlowService.checkoutAndInitiate(user.getUserId(), request)));
    }

    @GetMapping("/subscriptions/plans")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.success("Subscription plans retrieved", subscriptionService.listAvailablePlans()));
    }

    @PostMapping("/subscriptions/activate")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> activateSubscription(@AuthenticationPrincipal User user,
            @Valid @RequestBody ActivateSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription activated",
                subscriptionService.activatePlan(user.getUserId(), request.getSubscriptionPlanId())));
    }

    @GetMapping("/subscriptions/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> mySubscriptions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("User subscriptions retrieved",
                subscriptionService.getUserSubscriptions(user.getUserId())));
    }

    @GetMapping("/public-library")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PublicLibraryResponse>>> publicLibrary() {
        return ResponseEntity.ok(ApiResponse.success("Public library retrieved", publicLibraryService.listPublicItems()));
    }

    @GetMapping("/library")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<LibraryResponse>>> userLibrary(@AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("User library retrieved",
                userLibraryService.getUserLibrary(user.getUserId(), null, null, pageable)));
    }

    @PostMapping("/library/{bookId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<LibraryResponse>> addToLibrary(@AuthenticationPrincipal User user,
            @PathVariable UUID bookId,
            @RequestBody(required = false) Map<String, String> body) {
        String accessType = body != null && body.containsKey("accessType") ? body.get("accessType") : "sample";
        return ResponseEntity.ok(ApiResponse.success("Book added to user library",
                userLibraryService.addToLibrary(user.getUserId(), bookId, accessType, null)));
    }

    @DeleteMapping("/library/{bookId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeFromLibrary(@AuthenticationPrincipal User user,
            @PathVariable UUID bookId) {
        userLibraryService.removeFromLibrary(user.getUserId(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book removed from user library", null));
    }
}
