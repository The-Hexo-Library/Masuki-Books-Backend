package com.masukibooks.service;

import com.masukibooks.dto.response.BookReaderMetadata;
import com.masukibooks.dto.response.PageContentResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalContentService {

    private final ProductRepository productRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final BookmarkRepository bookmarkRepository;
    private final S3Client s3Client;

    @Value("${storage.s3.bucket}")
    private String bucket;

    public BookReaderMetadata getBookMetadata(UUID productId, UUID userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!"digital".equals(product.getContentType()) && !"both".equals(product.getContentType())) {
            throw new BusinessException("This product is not a digital book");
        }

        BookReaderMetadata.ProgressInfo progressInfo = null;
        List<BookReaderMetadata.BookmarkInfo> bookmarkInfos = List.of();

        if (userId != null) {
            progressInfo = readingProgressRepository
                    .findByUserUserIdAndProductProductId(userId, productId)
                    .map(rp -> BookReaderMetadata.ProgressInfo.builder()
                            .currentPage(rp.getCurrentPage())
                            .percentage(rp.getPercentage())
                            .lastReadAt(rp.getLastReadAt())
                            .readingTimeSeconds(rp.getReadingTimeSeconds())
                            .build())
                    .orElse(null);

            bookmarkInfos = bookmarkRepository
                    .findByUserUserIdAndProductProductIdOrderByPageNumberAsc(userId, productId)
                    .stream()
                    .map(b -> BookReaderMetadata.BookmarkInfo.builder()
                            .bookmarkId(b.getBookmarkId())
                            .pageNumber(b.getPageNumber())
                            .title(b.getTitle())
                            .note(b.getNote())
                            .color(b.getColor())
                            .build())
                    .collect(Collectors.toList());
        }

        // String coverImageUrl = product.getImages() != null && !product.getImages().isEmpty()
        //         ? product.getImages().stream()
        //             .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
        //             .map(ProductImage::getUrl)
        //             .findFirst()
        //             .orElse(product.getImages().get(0).getUrl())
        //         : null;

        return BookReaderMetadata.builder()
                .productId(product.getProductId())
                .title(product.getTitle())
                .author(product.getAuthor())
                .fileFormat(product.getFileFormat())
                .totalPages(product.getTotalPages())
                .previewPages(product.getPreviewPages())
                // .coverImageUrl(coverImageUrl)
                .currentProgress(progressInfo)
                .bookmarks(bookmarkInfos)
                .build();
    }

    public byte[] getPageContent(UUID productId, int pageNumber, UUID userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        verifyContentAccess(userId, product, pageNumber);
        validatePageNumber(product, pageNumber);

        String pageKey = buildPageKey(productId, pageNumber, product.getFileFormat());

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(pageKey)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
            return response.readAllBytes();
        } catch (S3Exception e) {
            log.error("Failed to fetch page {} for book {}: {}", pageNumber, productId, e.getMessage());
            throw new BusinessException("Failed to load page content");
        } catch (IOException e) {
            log.error("Failed to read page content for book {}: {}", productId, e.getMessage());
            throw new BusinessException("Failed to read page content");
        }
    }

    public List<PageContentResponse> getPageRange(UUID productId, int startPage, int endPage, UUID userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        int maxRange = 5;
        if (endPage - startPage + 1 > maxRange) {
            throw new BusinessException("Maximum " + maxRange + " pages per range request");
        }

        validatePageNumber(product, startPage);
        validatePageNumber(product, endPage);

        List<PageContentResponse> pages = new ArrayList<>();
        String contentType = "pdf".equals(product.getFileFormat()) ? "application/pdf" : "text/html";

        for (int page = startPage; page <= endPage; page++) {
            verifyContentAccess(userId, product, page);
            String pageKey = buildPageKey(productId, page, product.getFileFormat());

            try {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(pageKey)
                        .build();

                ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
                byte[] bytes = response.readAllBytes();
                String encoded = Base64.getEncoder().encodeToString(bytes);

                pages.add(PageContentResponse.builder()
                        .pageNumber(page)
                        .content(encoded)
                        .contentType(contentType)
                        .build());
            } catch (S3Exception | IOException e) {
                log.warn("Failed to fetch page {} for book {}: {}", page, productId, e.getMessage());
                pages.add(PageContentResponse.builder()
                        .pageNumber(page)
                        .content(null)
                        .contentType(contentType)
                        .build());
            }
        }

        return pages;
    }

    public void verifyContentAccess(UUID userId, Product product, int requestedPage) {
        int previewPages = product.getPreviewPages() != null ? product.getPreviewPages() : 0;
        if (requestedPage <= previewPages) {
            return;
        }

        if (userId == null) {
            throw new BusinessException("Authentication required to access this content");
        }

        boolean hasAccess = userLibraryRepository
                .existsByUserUserIdAndProductProductIdAndStatusIn(
                        userId, product.getProductId(), List.of("active"));

        if (!hasAccess) {
            throw new BusinessException("You do not have access to this book. Please purchase it first.");
        }
    }

    public boolean hasAccess(UUID userId, UUID productId) {
        if (userId == null) return false;
        return userLibraryRepository.existsByUserUserIdAndProductProductIdAndStatusIn(
                userId, productId, List.of("active"));
    }

    private void validatePageNumber(Product product, int pageNumber) {
        if (pageNumber < 1) {
            throw new BusinessException("Page number must be at least 1");
        }
        if (product.getTotalPages() != null && pageNumber > product.getTotalPages()) {
            throw new BusinessException("Page number exceeds total pages (" + product.getTotalPages() + ")");
        }
    }

    private String buildPageKey(UUID productId, int pageNumber, String fileFormat) {
        String extension = "epub".equals(fileFormat) ? "html" : "pdf";
        return String.format("digital-books/%s/pages/page-%03d.%s", productId, pageNumber, extension);
    }
}
