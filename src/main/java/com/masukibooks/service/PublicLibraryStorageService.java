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
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLibraryStorageService {

    private final ObjectMapper objectMapper;

    @Value("${storage.s3.endpoint:}")
    private String endpoint;

    @Value("${storage.s3.region:ap-northeast-2}")
    private String region;

    @Value("${storage.s3.access-key:}")
    private String accessKey;

    @Value("${storage.s3.secret-key:}")
    private String secretKey;

    @Value("${storage.s3.bucket:Test_bucket}")
    private String bucket;

    @Value("${storage.s3.public-library-prefix:public-library}")
    private String prefix;

    @Value("${storage.s3.public-url-prefix:}")
    private String publicUrlPrefix;

    /**
     * Creates the folder structure and uploads both files to Supabase S3.
     */
    public void uploadBook(String bookTitle, MultipartFile pdfFile, BooksMetadata metadata) {
        if (bookTitle == null || bookTitle.isBlank()) {
            throw new BusinessException("Book title is required for upload");
        }
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new BusinessException("PDF file is required for upload");
        }
        if (metadata == null) {
            throw new BusinessException("Book metadata is required for upload");
        }

        String safeTitle = sanitizeTitle(bookTitle);
        String folderPath = buildFolderPath(safeTitle);
        String pdfKey = folderPath + "/pdf/" + safeTitle + ".pdf";
        String metadataKey = folderPath + "/" + safeTitle + "-metadata.json";

        try (S3Client client = buildClient()) {
            // Upload PDF file
            PutObjectRequest pdfRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(pdfKey)
                    .contentType("application/pdf")
                    .build();
            client.putObject(pdfRequest, RequestBody.fromInputStream(pdfFile.getInputStream(), pdfFile.getSize()));
            log.info("Successfully uploaded PDF for book: {}", safeTitle);

            // Serialize Metadata to JSON and Upload
            String metadataJson = objectMapper.writeValueAsString(metadata);
            PutObjectRequest metadataRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(metadataKey)
                    .contentType("application/json")
                    .build();
            client.putObject(metadataRequest, RequestBody.fromString(metadataJson));
            log.info("Successfully uploaded metadata JSON for book: {}", safeTitle);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize BookMetadata to JSON for book: {}", safeTitle, e);
            throw new BusinessException("Failed to process book metadata format: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read PDF file input stream for book: {}", safeTitle, e);
            throw new BusinessException("Failed to process uploaded PDF file: " + e.getMessage());
        } catch (S3Exception e) {
            log.error("S3 storage error while uploading book: {}", safeTitle, e);
            throw new BusinessException("Storage operation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while uploading book: {}", safeTitle, e);
            throw new BusinessException("An unexpected error occurred during upload");
        }
    }

    /**
     * Deletes the entire folder (prefix) for that book from S3.
     */
    public void deleteBook(String bookTitle) {
        if (bookTitle == null || bookTitle.isBlank()) {
            return;
        }
        String safeTitle = sanitizeTitle(bookTitle);
        String folderPath = buildFolderPath(safeTitle) + "/";

        try (S3Client client = buildClient()) {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(folderPath)
                    .build();

            ListObjectsV2Response listResponse = client.listObjectsV2(listRequest);
            List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                    .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                    .collect(Collectors.toList());

            if (!objectsToDelete.isEmpty()) {
                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(objectsToDelete).build())
                        .build();
                client.deleteObjects(deleteObjectsRequest);
                log.info("Successfully deleted folder and its contents for book: {}", safeTitle);
            }
        } catch (S3Exception e) {
            log.error("S3 storage error while deleting book: {}", safeTitle, e);
            throw new BusinessException("Failed to delete book from storage: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while deleting book: {}", safeTitle, e);
            throw new BusinessException("An unexpected error occurred during deletion");
        }
    }

    /**
     * Returns the public S3 URL for the PDF.
     */
    public String getBookPdfUrl(String bookTitle) {
        if (bookTitle == null || bookTitle.isBlank()) return null;
        String safeTitle = sanitizeTitle(bookTitle);
        String pdfKey = buildFolderPath(safeTitle) + "/pdf/" + safeTitle + ".pdf";
        return resolvePublicUrl(pdfKey);
    }

    /**
     * Returns the public S3 URL for the metadata JSON.
     */
    public String getBookMetadataUrl(String bookTitle) {
        if (bookTitle == null || bookTitle.isBlank()) return null;
        String safeTitle = sanitizeTitle(bookTitle);
        String metadataKey = buildFolderPath(safeTitle) + "/" + safeTitle + "-metadata.json";
        return resolvePublicUrl(metadataKey);
    }

    /**
     * Sanitizes the title for S3 key usage by making it lowercase and replacing spaces/special characters with hyphens.
     */
    public String sanitizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-");
    }

    private String buildFolderPath(String safeTitle) {
        String cleanPrefix = (prefix != null && prefix.endsWith("/")) ? prefix.substring(0, prefix.length() - 1) : prefix;
        return cleanPrefix + "/" + safeTitle;
    }

    private String resolvePublicUrl(String objectKey) {
        if (publicUrlPrefix == null || publicUrlPrefix.isBlank()) {
            return endpoint + "/" + bucket + "/" + objectKey;
        }
        String cleanPrefix = publicUrlPrefix.endsWith("/")
                ? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
                : publicUrlPrefix;
        return cleanPrefix + "/" + objectKey;
    }

    private S3Client buildClient() {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank() || bucket == null || bucket.isBlank()) {
            throw new BusinessException("S3 Storage configuration is missing or incomplete.");
        }
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}