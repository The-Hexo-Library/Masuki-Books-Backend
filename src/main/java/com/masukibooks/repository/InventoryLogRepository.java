package com.masukibooks.repository;

import com.masukibooks.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, UUID> {
    List<InventoryLog> findByProductProductIdOrderByCreatedAtDesc(UUID productId);
}
