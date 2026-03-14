package com.masukibooks.repository;

import com.masukibooks.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findByOrderOrderId(UUID orderId);
    Optional<Shipment> findTopByOrderOrderIdOrderByCreatedAtDesc(UUID orderId);
}
