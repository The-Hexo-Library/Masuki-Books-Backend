package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.entity.ProductImage;
import com.masukibooks.service.ProductService;
import com.masukibooks.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final ProductService productService;

    /**
     * Upload a book cover image and link it to a product.
     */
    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('admin','superadmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            @RequestParam(value = "sortOrder", defaultValue = "0") int sortOrder) {

        String url = storageService.uploadFile(file, "covers/" + productId);
        ProductImage image = productService.addProductImage(productId, url, isPrimary, sortOrder);

        Map<String, Object> result = Map.of(
                "imageId", image.getImageId(),
                "url", url,
                "isPrimary", image.getIsPrimary(),
                "displayOrder", image.getDisplayOrder()
        );
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", result));
    }

    /**
     * Upload a general file to a specified folder (e.g., ebooks, audiobooks).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('admin','superadmin')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        String url = storageService.uploadFile(file, folder);
        return ResponseEntity.ok(ApiResponse.success("File uploaded", Map.of("url", url)));
    }

    /**
     * List files in a folder.
     */
    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<String>>> listFiles(
            @RequestParam(value = "folder", defaultValue = "") String folder) {
        return ResponseEntity.ok(ApiResponse.success("Files listed", storageService.listFiles(folder)));
    }

    /**
     * Delete a file by its key.
     */
    @DeleteMapping("/files")
    @PreAuthorize("hasAnyRole('admin','superadmin')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam("key") String key) {
        storageService.deleteFile(key);
        return ResponseEntity.ok(ApiResponse.success("File deleted", null));
    }
}
