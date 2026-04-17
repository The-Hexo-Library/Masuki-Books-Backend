package com.masukibooks.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masukibooks.entity.BooksMetadata;
import com.masukibooks.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookStorageService {

    private static final long MAX_REMOTE_FILE_BYTES = 50L * 1024L * 1024L;

    private final ObjectMapper objectMapper;

    @Value("${storage.s3.endpoint:}")
    private String endpoint;

    @Value("${storage.s3.region:ap-northeast-2}")
    private String region;

    @Value("${storage.s3.access-key:}")
    private String accessKey;

    @Value("${storage.s3.secret-key:}")
    private String secretKey;

    @Value("${storage.s3.bucket:books}")
    private String bucket;

    @Value("${storage.s3.object-key-prefix:storage/manage/books}")
    private String objectKeyPrefix;

    @Value("${storage.s3.public-url-prefix:}")
    private String publicUrlPrefix;

    public String uploadBookMetadata(BooksMetadata product) {
        if (product == null || product.getProductId() == null) {
            throw new BusinessException("Cannot upload metadata for an unsaved product.");
        }

        if (isBlank(endpoint) || isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket)) {
            throw new BusinessException("S3 storage is not configured. Set storage.s3 endpoint, access-key, secret-key, and bucket.");
        }

        String objectKey = buildMetadataKey(product.getProductId());
        String payload = toJsonPayload(product);

        try (S3Client client = buildClient()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType("application/json")
                    .build();

            client.putObject(request, RequestBody.fromString(payload));
            return objectKey;
        } catch (S3Exception ex) {
            log.error("S3 upload failed for product {}: {}", product.getProductId(), ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
            throw new BusinessException("Failed to upload book metadata to S3 bucket.");
        } catch (Exception ex) {
            log.error("Unexpected storage error for product {}", product.getProductId(), ex);
            throw new BusinessException("Failed to upload book metadata to S3 bucket.");
        }
    }

    public String uploadBookFile(UUID productId, MultipartFile file) {
        if (productId == null) {
            throw new BusinessException("Product id is required for file upload.");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Book file is required.");
        }
        validateStorageConfig();

        String objectKey = buildFileKey(productId, file.getOriginalFilename());
        String contentType = resolveContentType(file);

        try (S3Client client = buildClient()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return objectKey;
        } catch (S3Exception ex) {
            log.error("S3 file upload failed for product {}: {}", productId, ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
            throw new BusinessException("Failed to upload book file to S3 bucket.");
        } catch (IOException ex) {
            throw new BusinessException("Failed to read uploaded book file.");
        } catch (Exception ex) {
            log.error("Unexpected file storage error for product {}", productId, ex);
            throw new BusinessException("Failed to upload book file to S3 bucket.");
        }
    }

    public ImportedBookFile importBookFileFromUrl(UUID productId, String sourceUrl, String preferredFormat) {
        if (productId == null) {
            throw new BusinessException("Product id is required for file import.");
        }
        if (isBlank(sourceUrl)) {
            throw new BusinessException("Book URL is required.");
        }
        validateStorageConfig();

        URI sourceUri;
        try {
            sourceUri = URI.create(sourceUrl.trim());
        } catch (Exception ex) {
            throw new BusinessException("Book URL is invalid.");
        }

        String scheme = sourceUri.getScheme() == null ? "" : sourceUri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new BusinessException("Book URL must start with http:// or https://.");
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder(sourceUri)
                .timeout(Duration.ofSeconds(45))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new BusinessException("Book URL returned HTTP " + status + ".");
            }

            byte[] fileBytes = response.body();
            if (fileBytes == null || fileBytes.length == 0) {
                throw new BusinessException("Book URL did not return any file content.");
            }
            if (fileBytes.length > MAX_REMOTE_FILE_BYTES) {
                throw new BusinessException("Book file exceeds 50 MB size limit.");
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
            String fileFormat = resolveRemoteFormat(sourceUri, preferredFormat, contentType);
            String filename = "book-file." + fileFormat;
            String objectKey = buildFileKey(productId, filename);

            try (S3Client s3Client = buildClient()) {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(resolveRemoteContentType(contentType, fileFormat))
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
            }

            return new ImportedBookFile(objectKey, (long) fileBytes.length, fileFormat, resolveRemoteContentType(contentType, fileFormat));
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("Failed to read book content from URL.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Book URL import was interrupted.");
        } catch (S3Exception ex) {
            log.error("S3 import upload failed for product {}: {}", productId,
                    ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
            throw new BusinessException("Failed to store imported book file in S3.");
        } catch (Exception ex) {
            log.error("Unexpected URL import error for product {}", productId, ex);
            throw new BusinessException("Failed to import book file from URL.");
        }
    }

    public void deleteBookAssets(UUID productId, String fileKey) {
        if (productId == null || isBlank(endpoint) || isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket)) {
            return;
        }

        String metadataKey = buildMetadataKey(productId);
        try (S3Client client = buildClient()) {
            deleteObjectQuietly(client, metadataKey);
            if (!isBlank(fileKey) && !metadataKey.equals(fileKey)) {
                deleteObjectQuietly(client, fileKey);
            }
        } catch (Exception ex) {
            log.error("Unexpected file storage error while deleting assets for product {}", productId, ex);
            throw new BusinessException("Failed to delete book assets from storage.");
        }
    }

    private S3Client buildClient() {
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private String buildMetadataKey(UUID productId) {
        return cleanPrefix(objectKeyPrefix) + "/" + productId + "/metadata.json";
    }

    private String buildFileKey(UUID productId, String originalFilename) {
        String safeName = (originalFilename == null || originalFilename.isBlank())
                ? "book-file"
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleanPrefix(objectKeyPrefix) + "/" + productId + "/files/" + UUID.randomUUID() + "-" + safeName;
    }

    public String resolvePublicUrl(String objectKey) {
        if (isBlank(objectKey)) {
            return null;
        }

        String normalizedExternal = normalizeExternalUrl(objectKey);
        if (normalizedExternal != null) {
            return normalizedExternal;
        }

        if (isBlank(publicUrlPrefix)) {
            return null;
        }

        String cleanPrefix = publicUrlPrefix.endsWith("/")
                ? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
                : publicUrlPrefix;
        String cleanKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return cleanPrefix + "/" + cleanKey;
    }

    /**
     * Download a book file from S3. Returns an InputStream that can be streamed to the client.
     * The caller is responsible for closing the input stream.
     */
    public InputStream downloadBookFile(String fileKey) {
        if (isBlank(fileKey)) {
            throw new BusinessException("File key is required for download.");
        }
        validateStorageConfig();

        try {
            S3Client client = buildClient();
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            return client.getObject(request);
        } catch (S3Exception ex) {
            log.error("S3 file download failed for key {}: {}", fileKey, ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
            throw new BusinessException("Failed to download book file from S3 bucket.");
        } catch (Exception ex) {
            log.error("Unexpected error downloading file {}", fileKey, ex);
            throw new BusinessException("Failed to download book file.");
        }
    }

    private String toJsonPayload(BooksMetadata product) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getProductId());
        payload.put("sku", product.getSku());
        payload.put("title", product.getTitle());
        payload.put("author", product.getAuthor());
        payload.put("publisher", product.getPublisher());
        payload.put("isbn", product.getIsbn());
        payload.put("description", product.getDescription());
        payload.put("language", product.getLanguage());
        payload.put("format", product.getFormat());
        payload.put("pages", product.getPages());
        payload.put("publicationDate", product.getPublicationDate());
        payload.put("price", product.getPrice());
        payload.put("compareAtPrice", product.getCompareAtPrice());
        payload.put("status", product.getStatus());
        payload.put("contentType", product.getContentType());
        payload.put("createdAt", product.getCreatedAt());
        payload.put("updatedAt", product.getUpdatedAt());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Failed to serialize book metadata for S3 upload.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String cleanPrefix(String value) {
        if (isBlank(value)) {
            return "storage/manage/books";
        }
        String trimmed = value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String resolveContentType(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        }
        if (originalName != null && originalName.toLowerCase().endsWith(".epub")) {
            return "application/epub+zip";
        }
        String contentType = file.getContentType();
        return isBlank(contentType) ? "application/octet-stream" : contentType;
    }

    private String resolveRemoteFormat(URI sourceUri, String preferredFormat, String remoteContentType) {
        if (!isBlank(preferredFormat)) {
            String normalized = preferredFormat.trim().toLowerCase();
            if ("pdf".equals(normalized) || "epub".equals(normalized)) {
                return normalized;
            }
            if ("flipbook".equals(normalized) || "ebook".equals(normalized)) {
                return "pdf";
            }
        }

        if (remoteContentType != null) {
            String lower = remoteContentType.toLowerCase();
            if (lower.contains("application/pdf")) {
                return "pdf";
            }
            if (lower.contains("application/epub+zip")) {
                return "epub";
            }
        }

        String path = sourceUri.getPath() == null ? "" : sourceUri.getPath().toLowerCase();
        if (path.endsWith(".pdf")) {
            return "pdf";
        }
        if (path.endsWith(".epub")) {
            return "epub";
        }

        return "pdf";
    }

    private String resolveRemoteContentType(String remoteContentType, String fileFormat) {
        if (!isBlank(remoteContentType)
                && (remoteContentType.toLowerCase().contains("application/pdf")
                || remoteContentType.toLowerCase().contains("application/epub+zip"))) {
            return remoteContentType;
        }
        return "epub".equals(fileFormat) ? "application/epub+zip" : "application/pdf";
    }

    private void validateStorageConfig() {
        if (isBlank(endpoint) || isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket)) {
            throw new BusinessException("S3 storage is not configured. Set storage.s3 endpoint, access-key, secret-key, and bucket.");
        }
    }

    private void deleteObjectQuietly(S3Client client, String objectKey) {
        if (isBlank(objectKey)) {
            return;
        }
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            client.deleteObject(request);
        } catch (S3Exception ex) {
            log.warn("S3 delete failed for key {}: {}", objectKey,
                    ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
        }
    }

    private boolean isExternalUrl(String value) {
        return normalizeExternalUrl(value) != null;
    }

    private String normalizeExternalUrl(String value) {
        if (isBlank(value)) {
            return null;
        }

        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }

        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }

        if (lower.startsWith("www.")) {
            return "https://" + trimmed;
        }

        if (trimmed.matches("^[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+(?:/.*)?$")) {
            return "https://" + trimmed;
        }

        return null;
    }

    public record ImportedBookFile(String fileKey, Long fileSizeBytes, String fileFormat, String contentType) {
    }
}
