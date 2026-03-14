package com.masukibooks.repository;

import com.masukibooks.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductProductIdAndStatus(UUID productId, String status, Pageable pageable);
    Page<Review> findByStatus(String status, Pageable pageable);
    boolean existsByUserUserIdAndProductProductIdAndOrderOrderId(UUID userId, UUID productId, UUID orderId);
    boolean existsByUserUserIdAndProductProductId(UUID userId, UUID productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId AND r.status = 'approved'")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);
}
