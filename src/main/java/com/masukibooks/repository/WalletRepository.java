package com.masukibooks.repository;

import com.masukibooks.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUserUserId(UUID userId);
    boolean existsByUserUserId(UUID userId);
}
