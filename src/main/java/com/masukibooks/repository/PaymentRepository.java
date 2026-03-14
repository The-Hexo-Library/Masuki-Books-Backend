package com.masukibooks.repository;

import com.masukibooks.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderOrderId(UUID orderId);
    Optional<Payment> findTopByOrderOrderIdOrderByCreatedAtDesc(UUID orderId);
}
