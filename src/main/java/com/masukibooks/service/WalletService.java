package com.masukibooks.service;

import com.masukibooks.dto.response.WalletResponse;
import com.masukibooks.dto.response.WalletTransactionResponse;
import com.masukibooks.entity.User;
import com.masukibooks.entity.Wallet;
import com.masukibooks.entity.WalletTransaction;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.UserRepository;
import com.masukibooks.repository.WalletRepository;
import com.masukibooks.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByUserUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .balance(BigDecimal.ZERO)
                    .currency("INR")
                    .status("active")
                    .build();
            return walletRepository.save(wallet);
        });
    }

    public WalletResponse getWallet(UUID userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return mapWallet(wallet);
    }

    @Transactional
    public WalletResponse topUp(UUID userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessException("Minimum top-up amount is 1.00");
        }

        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        recordTransaction(wallet, "credit", amount, "Wallet top-up", "topup", null);

        log.info("Wallet top-up: userId={}, amount={}, newBalance={}", userId, amount, wallet.getBalance());
        return mapWallet(wallet);
    }

    @Transactional
    public void credit(UUID userId, BigDecimal amount, String description, String referenceType, UUID referenceId) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet, "credit", amount, description, referenceType, referenceId);
    }

    @Transactional
    public void debit(UUID userId, BigDecimal amount, String description, String referenceType, UUID referenceId) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        recordTransaction(wallet, "debit", amount, description, referenceType, referenceId);
    }

    public Page<WalletTransactionResponse> getTransactions(UUID userId, Pageable pageable) {
        Wallet wallet = getOrCreateWallet(userId);
        return walletTransactionRepository
                .findByWalletWalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable)
                .map(this::mapTransaction);
    }

    private void recordTransaction(Wallet wallet, String type, BigDecimal amount,
                                   String description, String referenceType, UUID referenceId) {
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .txnType(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        walletTransactionRepository.save(txn);
    }

    private WalletResponse mapWallet(Wallet w) {
        return WalletResponse.builder()
                .walletId(w.getWalletId().toString())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .status(w.getStatus())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private WalletTransactionResponse mapTransaction(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .transactionId(t.getTransactionId().toString())
                .txnType(t.getTxnType())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .referenceType(t.getReferenceType())
                .referenceId(t.getReferenceId() != null ? t.getReferenceId().toString() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
