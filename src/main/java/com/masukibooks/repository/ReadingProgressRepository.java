package com.masukibooks.repository;

import com.masukibooks.entity.ReadingProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, UUID> {

    Optional<ReadingProgress> findByUserUserIdAndProductProductId(UUID userId, UUID productId);

    Page<ReadingProgress> findByUserUserIdOrderByLastReadAtDesc(UUID userId, Pageable pageable);
}
