package com.masukibooks.controller;

import com.masukibooks.dto.request.ReviewRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.entity.Review;
import com.masukibooks.security.JwtTokenProvider;
import com.masukibooks.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<Review>>> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved",
                reviewService.getApprovedReviews(productId, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('user')")
    public ResponseEntity<ApiResponse<Review>> submitReview(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReviewRequest request) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        return ResponseEntity.ok(ApiResponse.success("Review submitted", reviewService.submitReview(userId, request)));
    }
}
