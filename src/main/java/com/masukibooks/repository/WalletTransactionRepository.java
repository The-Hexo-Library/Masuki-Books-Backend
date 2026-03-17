package com.masukibooks.repository;

import com.masukibooks.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Page<WalletTransaction> findByWalletWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
