package com.masukibooks.repository;

import com.masukibooks.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartCartId(UUID cartId);
    Optional<CartItem> findByCartCartIdAndProductProductId(UUID cartId, UUID productId);
    void deleteByCartCartId(UUID cartId);
    void deleteByProductProductId(UUID productId);
}
