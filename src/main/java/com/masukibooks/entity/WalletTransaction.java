package com.masukibooks.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions",
       indexes = {
           @Index(name = "idx_wallet_txn_wallet", columnList = "wallet_id"),
           @Index(name = "idx_wallet_txn_created", columnList = "created_at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "txn_type", nullable = false, length = 20)
    private String txnType;  // credit, debit

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "reference_type", length = 30)
    private String referenceType;  // topup, purchase, resale_credit, resale_debit, refund

    @Column(name = "reference_id")
    private UUID referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
