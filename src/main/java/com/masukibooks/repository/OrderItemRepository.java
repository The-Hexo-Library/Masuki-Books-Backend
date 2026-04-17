package com.masukibooks.repository;

import com.masukibooks.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderOrderId(UUID orderId);

    @Modifying
    @Query("update OrderItem oi set oi.product = null where oi.product.productId = :productId")
    int clearProductReference(@Param("productId") UUID productId);
}
