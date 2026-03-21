package com.masukibooks.service;

import com.masukibooks.entity.Product;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalBookProcessingService {

    private final ProductRepository productRepository;
    private final S3Client s3Client;

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Transactional
    public Product uploadAndProcessContent(UUID productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException("File content type is required");
        }

        String fileFormat;
        if ("application/pdf".equals(contentType)) {
            fileFormat = "pdf";
        } else if ("application/epub+zip".equals(contentType)) {
            fileFormat = "epub";
        } else {
            throw new BusinessException("Only PDF and EPUB files are supported. Received: " + contentType);
        }

        try {
            // Upload original file to S3
            String originalKey = String.format("digital-books/%s/original.%s", productId, fileFormat);
            uploadToS3(originalKey, file.getBytes(), contentType);

            // Update product metadata
            product.setFileKey(originalKey);
            product.setFileFormat(fileFormat);
            product.setFileSizeBytes(file.getSize());
            product.setContentType(product.getContentType() == null ? "digital" : product.getContentType());

            if (product.getPreviewPages() == null) {
                product.setPreviewPages(10);
            }
            if (product.getDownloadable() == null) {
                product.setDownloadable(false);
            }

            // Process pages based on format
            if ("pdf".equals(fileFormat)) {
                int totalPages = processPdfPages(productId, file.getBytes());
                product.setTotalPages(totalPages);
                log.info("Processed PDF for product {}: {} pages extracted", productId, totalPages);
            } else {
                // EPUB: store as-is for now; chapter extraction can be added later
                product.setTotalPages(product.getPages());
                log.info("EPUB uploaded for product {}", productId);
            }

            return productRepository.save(product);

        } catch (IOException e) {
            log.error("Failed to process file for product {}: {}", productId, e.getMessage());
            throw new BusinessException("Failed to process uploaded file: " + e.getMessage());
        }
    }

    private int processPdfPages(UUID productId, byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            log.info("Splitting PDF into {} individual pages for product {}", totalPages, productId);

            for (int i = 0; i < totalPages; i++) {
                try (PDDocument singlePageDoc = new PDDocument()) {
                    PDPage page = document.getPage(i);
                    singlePageDoc.addPage(page);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    singlePageDoc.save(baos);
                    byte[] pageBytes = baos.toByteArray();

                    String pageKey = String.format("digital-books/%s/pages/page-%03d.pdf", productId, i + 1);
                    uploadToS3(pageKey, pageBytes, "application/pdf");
                }

                if ((i + 1) % 50 == 0) {
                    log.info("Processed {}/{} pages for product {}", i + 1, totalPages, productId);
                }
            }

            return totalPages;
        }
    }

    private void uploadToS3(String key, byte[] content, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            log.error("S3 upload failed for key {}: {}", key, e.awsErrorDetails().errorMessage());
            throw new BusinessException("Failed to upload to storage: " + e.awsErrorDetails().errorMessage());
        }
    }
}
