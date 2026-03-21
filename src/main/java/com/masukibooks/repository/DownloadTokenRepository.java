package com.masukibooks.repository;

import com.masukibooks.entity.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DownloadTokenRepository extends JpaRepository<DownloadToken, UUID> {

    Optional<DownloadToken> findByTokenAndUsedFalse(String token);

    long countByUserUserIdAndProductProductIdAndUsedTrue(UUID userId, UUID productId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
