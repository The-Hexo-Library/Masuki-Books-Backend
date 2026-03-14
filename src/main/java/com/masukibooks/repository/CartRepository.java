package com.masukibooks.repository;

import com.masukibooks.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserUserIdAndStatus(UUID userId, String status);
    Optional<Cart> findByGuestTokenAndStatus(String guestToken, String status);
}
