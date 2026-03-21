package com.masukibooks.service;

import com.masukibooks.dto.request.ResaleListRequest;
import com.masukibooks.dto.response.ResaleListingResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResaleService {

    private final BookResaleRepository resaleRepository;
    private final UserLibraryRepository libraryRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    private static final long MIN_OWNERSHIP_DAYS = 7;
    private static final long MIN_READING_TIME_SECONDS = 3600; // 1 hour
    private static final BigDecimal MAX_RESALE_PERCENTAGE = new BigDecimal("0.70"); // 70% of original

    public Page<ResaleListingResponse> getMarketplace(Pageable pageable) {
        return resaleRepository.findByStatus("listed", pageable).map(this::mapResale);
    }

    public Page<ResaleListingResponse> getMyListings(UUID userId, Pageable pageable) {
        return resaleRepository.findBySellerUserId(userId, pageable).map(this::mapResale);
    }

    @Transactional
    public ResaleListingResponse listForResale(UUID sellerId, ResaleListRequest request) {
        UUID libraryId = UUID.fromString(request.getUserLibraryId());

        UserLibrary lib = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));

        if (!lib.getUser().getUserId().equals(sellerId)) {
            throw new BusinessException("You can only resell your own books");
        }

        if (!"purchased".equals(lib.getAccessType())) {
            throw new BusinessException("Only purchased books can be resold");
        }

        if (!"active".equals(lib.getStatus())) {
            throw new BusinessException("Book is not in active status");
        }

        // Check already listed
        if (resaleRepository.existsByUserLibraryUserLibraryIdAndStatusIn(
                libraryId, List.of("listed"))) {
            throw new BusinessException("Book is already listed for resale");
        }

        // Check minimum ownership period
        long daysSincePurchase = Duration.between(lib.getAcquiredAt(), LocalDateTime.now()).toDays();
        if (daysSincePurchase < MIN_OWNERSHIP_DAYS) {
            throw new BusinessException(
                    "You must own the book for at least " + MIN_OWNERSHIP_DAYS + " days before reselling");
        }

        // Check minimum reading time
        long readingTime = readingProgressRepository
                .findByUserUserIdAndProductProductId(sellerId, lib.getProduct().getProductId())
                .map(ReadingProgress::getReadingTimeSeconds)
                .orElse(0L);

        if (readingTime < MIN_READING_TIME_SECONDS) {
            throw new BusinessException(
                    "You must read the book for at least 1 hour before reselling");
        }

        // Validate listing price (max 70% of original)
        BigDecimal maxPrice = lib.getProduct().getPrice()
                .multiply(MAX_RESALE_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
        if (request.getListingPrice().compareTo(maxPrice) > 0) {
            throw new BusinessException("Maximum resale price is " + maxPrice + " (70% of original)");
        }

        BookResale resale = BookResale.builder()
                .seller(lib.getUser())
                .product(lib.getProduct())
                .userLibrary(lib)
                .originalPrice(lib.getProduct().getPrice())
                .listingPrice(request.getListingPrice())
                .readingTimeSeconds(readingTime)
                .status("listed")
                .listedAt(LocalDateTime.now())
                .build();

        resaleRepository.save(resale);
        log.info("Book listed for resale: resaleId={}, productId={}", resale.getResaleId(),
                lib.getProduct().getProductId());
        return mapResale(resale);
    }

    @Transactional
    public ResaleListingResponse buyResale(UUID buyerId, UUID resaleId) {
        BookResale resale = resaleRepository.findByIdForUpdate(resaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Resale listing not found"));

        if (!"listed".equals(resale.getStatus())) {
            throw new BusinessException("This listing is no longer available");
        }

        if (resale.getSeller().getUserId().equals(buyerId)) {
            throw new BusinessException("You cannot buy your own listing");
        }

        // Check buyer doesn't already own this book
        if (libraryRepository.existsByUserUserIdAndProductProductIdAndStatusIn(
                buyerId, resale.getProduct().getProductId(), List.of("active"))) {
            throw new BusinessException("You already own this book");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        // Debit buyer wallet
        walletService.debit(buyerId, resale.getListingPrice(),
                "Purchased resale: " + resale.getProduct().getTitle(),
                "resale_debit", resale.getResaleId());

        // Credit seller wallet
        walletService.credit(resale.getSeller().getUserId(), resale.getListingPrice(),
                "Resale sold: " + resale.getProduct().getTitle(),
                "resale_credit", resale.getResaleId());

        // Transfer ownership: revoke from seller, grant to buyer
        resale.getUserLibrary().setStatus("revoked");
        libraryRepository.save(resale.getUserLibrary());

        UserLibrary buyerLib = UserLibrary.builder()
                .user(buyer)
                .product(resale.getProduct())
                .accessType("purchased")
                .acquiredAt(LocalDateTime.now())
                .status("active")
                .build();
        libraryRepository.save(buyerLib);

        // Mark resale as sold
        resale.setBuyer(buyer);
        resale.setStatus("sold");
        resale.setSoldAt(LocalDateTime.now());
        resaleRepository.save(resale);

        log.info("Resale completed: resaleId={}, buyer={}, seller={}",
                resaleId, buyerId, resale.getSeller().getUserId());
        return mapResale(resale);
    }

    @Transactional
    public void cancelListing(UUID sellerId, UUID resaleId) {
        BookResale resale = resaleRepository.findById(resaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Resale listing not found"));

        if (!resale.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException("You can only cancel your own listing");
        }

        if (!"listed".equals(resale.getStatus())) {
            throw new BusinessException("Only active listings can be cancelled");
        }

        resale.setStatus("cancelled");
        resaleRepository.save(resale);
    }

    private ResaleListingResponse mapResale(BookResale r) {
        Product p = r.getProduct();
        // String coverUrl = (p.getImages() != null && !p.getImages().isEmpty())
        // ? p.getImages().get(0).getUrl() : null;

        return ResaleListingResponse.builder()
                .resaleId(r.getResaleId().toString())
                .sellerId(r.getSeller().getUserId().toString())
                .sellerName(r.getSeller().getFirstName())
                .productId(p.getProductId().toString())
                .title(p.getTitle())
                .author(p.getAuthor())
                // .coverImageUrl(coverUrl)
                .originalPrice(r.getOriginalPrice())
                .listingPrice(r.getListingPrice())
                .readingTimeSeconds(r.getReadingTimeSeconds())
                .status(r.getStatus())
                .listedAt(r.getListedAt())
                .soldAt(r.getSoldAt())
                .build();
    }
}
