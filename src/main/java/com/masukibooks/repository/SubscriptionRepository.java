package com.masukibooks.repository;

import com.masukibooks.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByIsPlanTrueAndIsActiveTrueOrderByPriceAsc();
    List<Subscription> findByUserUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Subscription> findFirstByUserUserIdAndIsPlanFalseOrderByCreatedAtDesc(UUID userId);
}
