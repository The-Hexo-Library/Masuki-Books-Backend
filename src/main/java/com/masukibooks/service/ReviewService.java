package com.masukibooks.service;

import com.masukibooks.dto.request.ReviewRequest;
import com.masukibooks.entity.Product;
import com.masukibooks.entity.Review;
import com.masukibooks.entity.User;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.OrderRepository;
import com.masukibooks.repository.ProductRepository;
import com.masukibooks.repository.ReviewRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review submitReview(UUID userId, ReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify purchase
        boolean hasPurchased = orderRepository.findByUserUserId(userId, Pageable.unpaged())
                .stream()
                .anyMatch(o -> "delivered".equals(o.getStatus()) &&
                        o.getItems() != null &&
                        o.getItems().stream()
                                .anyMatch(i -> i.getProduct().getProductId().equals(request.getProductId())));
        if (!hasPurchased) {
            throw new BusinessException("You must purchase and receive this book before reviewing");
        }

        boolean alreadyReviewed = reviewRepository.existsByUserUserIdAndProductProductId(userId, request.getProductId());
        if (alreadyReviewed) {
            throw new BusinessException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating((short) request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .status("pending")
                .build();
        return reviewRepository.save(review);
    }

    public Page<Review> getApprovedReviews(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductProductIdAndStatus(productId, "approved", pageable);
    }

    public Page<Review> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus("pending", pageable);
    }

    @Transactional
    public Review moderateReview(UUID reviewId, String status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setStatus(status);
        return reviewRepository.save(review);
    }
}
