package com.masukibooks.service;

import com.masukibooks.dto.request.BookmarkRequest;
import com.masukibooks.dto.response.BookmarkResponse;
import com.masukibooks.dto.response.ReadingProgressResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingProgressService {

    private final ReadingProgressRepository readingProgressRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReadingProgressResponse saveProgress(UUID userId, UUID productId, int currentPage, long readingTimeSeconds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        int totalPages = product.getTotalPages() != null ? product.getTotalPages() : 0;
        if (currentPage < 1 || (totalPages > 0 && currentPage > totalPages)) {
            throw new BusinessException("Invalid page number");
        }

        BigDecimal percentage = totalPages > 0
                ? BigDecimal.valueOf(currentPage).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalPages), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ReadingProgress progress = readingProgressRepository
                .findByUserUserIdAndProductProductId(userId, productId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    return ReadingProgress.builder()
                            .user(user)
                            .product(product)
                            .readingTimeSeconds(0L)
                            .build();
                });

        progress.setCurrentPage(currentPage);
        progress.setTotalPages(totalPages);
        progress.setPercentage(percentage);
        progress.setLastReadAt(LocalDateTime.now());
        progress.setReadingTimeSeconds(progress.getReadingTimeSeconds() + readingTimeSeconds);

        progress = readingProgressRepository.save(progress);
        return toProgressResponse(progress);
    }

    public ReadingProgressResponse getProgress(UUID userId, UUID productId) {
        return readingProgressRepository
                .findByUserUserIdAndProductProductId(userId, productId)
                .map(this::toProgressResponse)
                .orElse(ReadingProgressResponse.builder()
                        .currentPage(0)
                        .totalPages(0)
                        .percentage(BigDecimal.ZERO)
                        .readingTimeSeconds(0L)
                        .build());
    }

    @Transactional
    public BookmarkResponse createBookmark(UUID userId, UUID productId, BookmarkRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (product.getTotalPages() != null && request.getPageNumber() > product.getTotalPages()) {
            throw new BusinessException("Page number exceeds total pages");
        }

        // Check for duplicate bookmark on same page
        bookmarkRepository.findByUserUserIdAndProductProductIdAndPageNumber(
                userId, productId, request.getPageNumber())
                .ifPresent(b -> {
                    throw new BusinessException("Bookmark already exists on this page");
                });

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .product(product)
                .pageNumber(request.getPageNumber())
                .title(request.getTitle())
                .note(request.getNote())
                .color(request.getColor())
                .build();

        bookmark = bookmarkRepository.save(bookmark);
        return toBookmarkResponse(bookmark);
    }

    public List<BookmarkResponse> getBookmarks(UUID userId, UUID productId) {
        return bookmarkRepository
                .findByUserUserIdAndProductProductIdOrderByPageNumberAsc(userId, productId)
                .stream()
                .map(this::toBookmarkResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBookmark(UUID userId, UUID bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found"));
        if (!bookmark.getUser().getUserId().equals(userId)) {
            throw new BusinessException("You can only delete your own bookmarks");
        }
        bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public BookmarkResponse updateBookmark(UUID userId, UUID bookmarkId, BookmarkRequest request) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found"));
        if (!bookmark.getUser().getUserId().equals(userId)) {
            throw new BusinessException("You can only update your own bookmarks");
        }

        if (request.getTitle() != null) bookmark.setTitle(request.getTitle());
        if (request.getNote() != null) bookmark.setNote(request.getNote());
        if (request.getColor() != null) bookmark.setColor(request.getColor());

        return toBookmarkResponse(bookmarkRepository.save(bookmark));
    }

    private ReadingProgressResponse toProgressResponse(ReadingProgress rp) {
        return ReadingProgressResponse.builder()
                .currentPage(rp.getCurrentPage())
                .totalPages(rp.getTotalPages())
                .percentage(rp.getPercentage())
                .lastReadAt(rp.getLastReadAt())
                .readingTimeSeconds(rp.getReadingTimeSeconds())
                .build();
    }

    private BookmarkResponse toBookmarkResponse(Bookmark b) {
        return BookmarkResponse.builder()
                .bookmarkId(b.getBookmarkId())
                .productId(b.getProduct().getProductId())
                .pageNumber(b.getPageNumber())
                .title(b.getTitle())
                .note(b.getNote())
                .color(b.getColor())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
