package com.masukibooks.repository;

import com.masukibooks.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByOrderOrderId(UUID orderId);
    List<Refund> findByStatus(String status);
    Page<Refund> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
