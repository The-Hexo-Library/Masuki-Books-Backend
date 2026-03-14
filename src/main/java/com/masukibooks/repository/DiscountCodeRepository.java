package com.masukibooks.repository;

import com.masukibooks.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {
    Optional<DiscountCode> findByCodeAndIsActiveTrue(String code);
    boolean existsByCode(String code);
}
