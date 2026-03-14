package com.masukibooks.service;

import com.masukibooks.dto.request.DiscountCodeRequest;
import com.masukibooks.entity.DiscountCode;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountCodeRepository discountCodeRepository;

    public DiscountCode validate(String code) {
        DiscountCode dc = discountCodeRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new BusinessException("Invalid or inactive discount code"));
        if (dc.getExpiresAt() != null && dc.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Discount code has expired");
        }
        if (dc.getMaxUses() != null && dc.getUsedCount() >= dc.getMaxUses()) {
            throw new BusinessException("Discount code usage limit reached");
        }
        return dc;
    }

    @Transactional
    public DiscountCode create(DiscountCodeRequest request) {
        if (discountCodeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Discount code already exists");
        }
        DiscountCode dc = DiscountCode.builder()
                .code(request.getCode())
                .type(request.getType())
                .value(request.getValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxUses(request.getMaxUses())
                .usedCount(0)
                .isActive(true)
                .startsAt(request.getStartsAt())
                .expiresAt(request.getExpiresAt())
                .build();
        return discountCodeRepository.save(dc);
    }

    @Transactional
    public DiscountCode toggle(UUID id, boolean active) {
        DiscountCode dc = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code not found"));
        dc.setIsActive(active);
        return discountCodeRepository.save(dc);
    }

    public Page<DiscountCode> listAll(Pageable pageable) {
        return discountCodeRepository.findAll(pageable);
    }
}
