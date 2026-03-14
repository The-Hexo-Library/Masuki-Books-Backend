package com.masukibooks.repository;

import com.masukibooks.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    Optional<OtpVerification> findTopByRecipientAndTypeAndUsedAtIsNullOrderByCreatedAtDesc(
        String recipient, String type
    );
}
