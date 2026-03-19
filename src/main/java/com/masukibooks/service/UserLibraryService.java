package com.masukibooks.service;

import com.masukibooks.dto.response.LibraryResponse;
import com.masukibooks.dto.response.RecentReadResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLibraryService {

    private final UserLibraryRepository userLibraryRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public Page<LibraryResponse> getUserLibrary(UUID userId, String status, String accessType, Pageable pageable) {
        Page<UserLibrary> records;

        if (status != null && accessType != null) {
            records = userLibraryRepository.findByUserUserIdAndStatusAndAccessType(userId, status, accessType,
                    pageable);
        } else if (status != null) {
            records = userLibraryRepository.findByUserUserIdAndStatus(userId, status, pageable);
        } else if (accessType != null) {
            records = userLibraryRepository.findByUserUserIdAndAccessType(userId, accessType, pageable);
        } else {
            records = userLibraryRepository.findByUserUserId(userId, pageable);
        }

        return records.map(this::toLibraryResponse);
    }

    @Transactional
    public LibraryResponse addToLibrary(UUID userId, UUID productId, String accessType, UUID orderId) {
        // Check if already in library
        var existing = userLibraryRepository.findByUserUserIdAndProductProductId(userId, productId);
        if (existing.isPresent()) {
            UserLibrary record = existing.get();
            if ("active".equals(record.getStatus())) {
                return toLibraryResponse(record);
            }
            // Re-activate if it was removed/expired
            record.setStatus("active");
            record.setAccessType(accessType);
            record.setAcquiredAt(LocalDateTime.now());
            if (orderId != null) {
                record.setOrder(orderRepository.findById(orderId).orElse(null));
            }
            return toLibraryResponse(userLibraryRepository.save(record));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!"digital".equals(product.getContentType()) && !"both".equals(product.getContentType())) {
            throw new BusinessException("Only digital products can be added to your library");
        }

        UserLibrary record = UserLibrary.builder()
                .user(user)
                .product(product)
                .accessType(accessType)
                .acquiredAt(LocalDateTime.now())
                .status("active")
                .build();

        if (orderId != null) {
            record.setOrder(orderRepository.findById(orderId).orElse(null));
        }

        return toLibraryResponse(userLibraryRepository.save(record));
    }

    public LibraryResponse getLibraryRecord(UUID userId, UUID productId) {
        UserLibrary record = userLibraryRepository
                .findByUserUserIdAndProductProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found in your library"));
        return toLibraryResponse(record);
    }

    public Page<RecentReadResponse> getRecentlyRead(UUID userId, Pageable pageable) {
        return readingProgressRepository
                .findByUserUserIdOrderByLastReadAtDesc(userId, pageable)
                .map(this::toRecentReadResponse);
    }

    @Transactional
    public void removeFromLibrary(UUID userId, UUID productId) {
        UserLibrary record = userLibraryRepository
                .findByUserUserIdAndProductProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found in your library"));
        record.setStatus("removed");
        userLibraryRepository.save(record);
    }

    @Transactional
    public void revokeAccess(UUID userId, UUID productId) {
        UserLibrary record = userLibraryRepository
                .findByUserUserIdAndProductProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Library record not found"));
        record.setStatus("revoked");
        userLibraryRepository.save(record);
    }

    private LibraryResponse toLibraryResponse(UserLibrary record) {
        Product product = record.getProduct();

        String coverImageUrl = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(ProductImage::getUrl)
                        .findFirst()
                        .orElse(product.getImages().get(0).getUrl())
                : null;

        var progress = readingProgressRepository
                .findByUserUserIdAndProductProductId(
                        record.getUser().getUserId(), product.getProductId())
                .orElse(null);

        return LibraryResponse.builder()
                .userLibraryId(record.getUserLibraryId())
                .productId(product.getProductId())
                .title(product.getTitle())
                .author(product.getAuthor())
                .coverImageUrl(coverImageUrl)
                .fileFormat(product.getFileFormat())
                .accessType(record.getAccessType())
                .acquiredAt(record.getAcquiredAt())
                .expiresAt(record.getExpiresAt())
                .status(record.getStatus())
                .currentPage(progress != null ? progress.getCurrentPage() : null)
                .totalPages(product.getTotalPages())
                .readingPercentage(progress != null ? progress.getPercentage() : null)
                .lastReadAt(progress != null ? progress.getLastReadAt() : null)
                .build();
    }

    private RecentReadResponse toRecentReadResponse(ReadingProgress rp) {
        Product product = rp.getProduct();

        String coverImageUrl = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(ProductImage::getUrl)
                        .findFirst()
                        .orElse(product.getImages().get(0).getUrl())
                : null;

        return RecentReadResponse.builder()
                .productId(product.getProductId())
                .title(product.getTitle())
                .author(product.getAuthor())
                .coverImageUrl(coverImageUrl)
                .fileFormat(product.getFileFormat())
                .lastReadAt(rp.getLastReadAt())
                .currentPage(rp.getCurrentPage())
                .totalPages(rp.getTotalPages())
                .percentage(rp.getPercentage())
                .build();
    }
}
