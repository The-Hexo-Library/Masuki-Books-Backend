package com.masukibooks.repository;

import com.masukibooks.entity.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, UUID> {
    Optional<ProcessedStripeEvent> findByEventId(String eventId);
}
