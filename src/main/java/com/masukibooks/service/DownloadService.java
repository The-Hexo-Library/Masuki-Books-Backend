package com.masukibooks.service;

import com.masukibooks.dto.response.DownloadTokenResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadService {

    private final DownloadTokenRepository downloadTokenRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Transactional
    public DownloadTokenResponse generateDownloadToken(UUID userId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!Boolean.TRUE.equals(product.getDownloadable())) {
            throw new BusinessException("This book is not available for download");
        }

        // Verify user owns the book
        boolean hasAccess = userLibraryRepository.existsByUserUserIdAndProductProductIdAndStatusIn(
                userId, productId, List.of("active"));
        if (!hasAccess) {
            throw new BusinessException("You do not have access to download this book");
        }

        // Check download limits
        int maxDownloads = product.getMaxDownloads() != null ? product.getMaxDownloads() : 3;
        long usedDownloads = downloadTokenRepository.countByUserUserIdAndProductProductIdAndUsedTrue(userId, productId);
        if (usedDownloads >= maxDownloads) {
            throw new BusinessException("Download limit reached (" + maxDownloads + " downloads)");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate secure token
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String tokenValue = HexFormat.of().formatHex(tokenBytes);

        DownloadToken token = DownloadToken.builder()
                .user(user)
                .product(product)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES))
                .used(false)
                .build();

        downloadTokenRepository.save(token);

        return DownloadTokenResponse.builder()
                .token(tokenValue)
                .expiresAt(token.getExpiresAt())
                .downloadsUsed(usedDownloads)
                .maxDownloads(maxDownloads)
                .build();
    }

    @Transactional
    public DownloadContext processDownload(String tokenValue, String ipAddress) {
        DownloadToken token = downloadTokenRepository.findByTokenAndUsedFalse(tokenValue)
                .orElseThrow(() -> new BusinessException("Invalid or expired download token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Download token has expired");
        }

        // Mark token as used
        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        token.setIpAddress(ipAddress);
        downloadTokenRepository.save(token);

        Product product = token.getProduct();
        String fileKey = product.getFileKey();
        if (fileKey == null || fileKey.isBlank()) {
            throw new BusinessException("No file available for this book");
        }

        String contentType = "epub".equals(product.getFileFormat())
                ? "application/epub+zip"
                : "application/pdf";

        String fileName = sanitizeFileName(product.getTitle()) + "." + product.getFileFormat();

        StreamingResponseBody body = outputStream -> {
            try {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileKey)
                        .build();

                ResponseInputStream<GetObjectResponse> s3Response = s3Client.getObject(getRequest);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = s3Response.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                s3Response.close();
            } catch (S3Exception e) {
                log.error("S3 download failed for product {}: {}", product.getProductId(), e.getMessage());
                throw new RuntimeException("Failed to stream file from storage");
            }
        };

        return new DownloadContext(body, contentType, fileName, product.getFileSizeBytes());
    }

    private String sanitizeFileName(String title) {
        if (title == null) return "book";
        return title.replaceAll("[^a-zA-Z0-9._\\- ]", "").trim().replaceAll("\\s+", "_");
    }

    public record DownloadContext(
            StreamingResponseBody body,
            String contentType,
            String fileName,
            Long fileSize
    ) {}
}
