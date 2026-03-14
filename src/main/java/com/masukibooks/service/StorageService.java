package com.masukibooks.service;

import com.masukibooks.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Value("${storage.s3.max-file-size}")
    private long maxFileSize;

    @Value("${storage.s3.public-url-prefix}")
    private String publicUrlPrefix;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "application/pdf", "application/epub+zip"
    );

    /**
     * Upload a file to the books bucket under a given folder.
     * Returns the public URL of the uploaded file.
     */
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String publicUrl = publicUrlPrefix + "/" + key;
            log.info("File uploaded successfully: {}", publicUrl);
            return publicUrl;

        } catch (IOException e) {
            log.error("Failed to read file for upload", e);
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        } catch (S3Exception e) {
            log.error("S3 upload failed: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException("Storage upload failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Delete a file from the bucket by its key (path after bucket name).
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("File deleted: {}", key);
        } catch (S3Exception e) {
            log.error("S3 delete failed: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException("Storage delete failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * List all files in a given folder prefix.
     */
    public List<String> listFiles(String folder) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(folder + "/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            return response.contents().stream()
                    .map(obj -> publicUrlPrefix + "/" + obj.key())
                    .toList();
        } catch (S3Exception e) {
            log.error("S3 list failed: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException("Storage list failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Extract the S3 key from a full public URL.
     */
    public String extractKeyFromUrl(String url) {
        if (url != null && url.startsWith(publicUrlPrefix + "/")) {
            return url.substring(publicUrlPrefix.length() + 1);
        }
        return url;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("File type not allowed. Accepted types: JPEG, PNG, WebP, GIF, PDF, EPUB");
        }
    }
}
