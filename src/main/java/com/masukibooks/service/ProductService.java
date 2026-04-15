package com.masukibooks.service;

import com.masukibooks.dto.request.ProductRequest;
import com.masukibooks.dto.response.ProductResponse;
import com.masukibooks.entity.BooksMetadata;
import com.masukibooks.entity.Category;
import com.masukibooks.entity.PublicLibrary;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.BooksMetadataRepository;
import com.masukibooks.repository.CartItemRepository;
import com.masukibooks.repository.CategoryRepository;
import com.masukibooks.repository.OrderItemRepository;
import com.masukibooks.repository.PublicLibraryRepository;
import com.masukibooks.repository.UserLibraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
// import java.util.List;
import java.util.UUID;
// import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final BooksMetadataRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PublicLibraryRepository publicLibraryRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookStorageService bookStorageService;
    private final SupabaseCatalogService supabaseCatalogService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, UUID categoryId,
            String language, BigDecimal minPrice,
            BigDecimal maxPrice, Pageable pageable) {
        return productRepository.searchProducts(keyword, categoryId, language,
                minPrice, maxPrice, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        BooksMetadata product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return productRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(UUID.fromString(request.getCategoryId()))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        BooksMetadata product = BooksMetadata.builder()
                .category(category)
                .isbn(request.getIsbn())
                .sku(request.getSku())
                .title(request.getTitle())
                .description(request.getDescription())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .format(request.getFormat())
                .pages(request.getPages())
                .publicationDate(request.getPublicationDate())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .status(request.getStatus() != null ? request.getStatus() : "draft")
                .contentType(request.getContentType() != null ? request.getContentType() : "physical")
                .fileFormat(request.getFileFormat())
                .fileSizeBytes(request.getFileSizeBytes())
                .totalPages(request.getTotalPages())
                .previewPages(request.getPreviewPages() != null ? request.getPreviewPages() : 10)
                .downloadable(request.getDownloadable() != null ? request.getDownloadable() : false)
                .maxDownloads(request.getMaxDownloads() != null ? request.getMaxDownloads() : 3)
                .build();

        BooksMetadata saved = productRepository.save(product);

        String metadataKey = bookStorageService.uploadBookMetadata(saved);
        saved.setFileKey(metadataKey);
        if (saved.getFileFormat() == null || saved.getFileFormat().isBlank()) {
            saved.setFileFormat("json");
        }

        saved = productRepository.save(saved);
        if ("published".equalsIgnoreCase(saved.getStatus())) {
            ensurePublicVisibility(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {
        BooksMetadata product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(UUID.fromString(request.getCategoryId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }
        if (request.getTitle() != null)
            product.setTitle(request.getTitle());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getAuthor() != null)
            product.setAuthor(request.getAuthor());
        if (request.getSku() != null)
            product.setSku(request.getSku());
        if (request.getPublisher() != null)
            product.setPublisher(request.getPublisher());
        if (request.getIsbn() != null)
            product.setIsbn(request.getIsbn());
        if (request.getFormat() != null)
            product.setFormat(request.getFormat());
        if (request.getPages() != null)
            product.setPages(request.getPages());
        if (request.getPublicationDate() != null)
            product.setPublicationDate(request.getPublicationDate());
        if (request.getPrice() != null)
            product.setPrice(request.getPrice());
        if (request.getCompareAtPrice() != null)
            product.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getLanguage() != null)
            product.setLanguage(request.getLanguage());
        if (request.getStatus() != null)
            product.setStatus(request.getStatus());

        // Digital content fields
        if (request.getContentType() != null)
            product.setContentType(request.getContentType());
        if (request.getFileFormat() != null)
            product.setFileFormat(request.getFileFormat());
        if (request.getFileSizeBytes() != null)
            product.setFileSizeBytes(request.getFileSizeBytes());
        if (request.getTotalPages() != null)
            product.setTotalPages(request.getTotalPages());
        if (request.getPreviewPages() != null)
            product.setPreviewPages(request.getPreviewPages());
        if (request.getDownloadable() != null)
            product.setDownloadable(request.getDownloadable());
        if (request.getMaxDownloads() != null)
            product.setMaxDownloads(request.getMaxDownloads());

        BooksMetadata saved = productRepository.save(product);
        if ("published".equalsIgnoreCase(saved.getStatus())) {
            ensurePublicVisibility(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        BooksMetadata product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Remove stored assets first so files are not orphaned in storage.
        bookStorageService.deleteBookAssets(product.getProductId(), product.getFileKey());

        // Clear dependent references before removing the product.
        publicLibraryRepository.deleteByProductProductId(productId);
        userLibraryRepository.deleteByProductProductId(productId);
        cartItemRepository.deleteByProductProductId(productId);
        orderItemRepository.clearProductReference(productId);
        supabaseCatalogService.deleteBookCatalogRow(productId);

        productRepository.delete(product);
    }

    @Transactional
    public ProductResponse uploadBookFile(UUID productId, MultipartFile file) {
        BooksMetadata product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Book file is required.");
        }

        String fileKey = bookStorageService.uploadBookFile(productId, file);
        product.setFileKey(fileKey);
        product.setFileSizeBytes(file.getSize());

        String ext = extractExtension(file.getOriginalFilename());
        if (!ext.isBlank()) {
            product.setFileFormat(ext);
        }

        if (product.getContentType() == null || product.getContentType().isBlank() || "physical".equalsIgnoreCase(product.getContentType())) {
            product.setContentType("digital");
        }

        BooksMetadata saved = productRepository.save(product);
        ensurePublicVisibility(saved);
        return toResponse(saved);
    }

    // @Transactional
    // public ProductImage addProductImage(UUID productId, String imageUrl, boolean
    // isPrimary, int sortOrder) {
    // Product product = productRepository.findById(productId)
    // .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    // ProductImage image = ProductImage.builder()
    // .product(product)
    // .url(imageUrl)
    // .isPrimary(isPrimary)
    // .displayOrder(sortOrder)
    // .build();
    // return productImageRepository.save(image);
    // }

    private ProductResponse toResponse(BooksMetadata p) {
        String catId = null;
        String catName = null;
        try {
            if (p.getCategory() != null) {
                catId = p.getCategory().getCategoryId().toString();
                catName = p.getCategory().getName();
            }
        } catch (Exception ignored) {
            // Lazy loading may fail with native queries
        }

        return ProductResponse.builder()
                .productId(p.getProductId())
                .categoryId(catId)
                .categoryName(catName)
                .isbn(p.getIsbn())
                .sku(p.getSku())
                .title(p.getTitle())
                .description(p.getDescription())
                .author(p.getAuthor())
                .publisher(p.getPublisher())
                .language(p.getLanguage())
                .format(p.getFormat())
                .pages(p.getPages())
                .publicationDate(p.getPublicationDate())
                .price(p.getPrice())
                .compareAtPrice(p.getCompareAtPrice())
                .status(p.getStatus())
                .stockQuantity(null)
                .inStock(true)
                .averageRating(null)
                .contentType(p.getContentType())
                .fileKey(p.getFileKey())
                .fileUrl(bookStorageService.resolvePublicUrl(p.getFileKey()))
                .fileFormat(p.getFileFormat())
                .fileSizeBytes(p.getFileSizeBytes())
                .totalPages(p.getTotalPages())
                .previewPages(p.getPreviewPages())
                .downloadable(Boolean.TRUE.equals(p.getDownloadable()))
                .maxDownloads(p.getMaxDownloads())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    private void ensurePublicVisibility(BooksMetadata product) {
        publicLibraryRepository.findByProductProductId(product.getProductId()).orElseGet(() -> {
            PublicLibrary record = PublicLibrary.builder()
                    .product(product)
                    .visibility("public")
                    .isFeatured(false)
                    .editable(true)
                    .build();
            return publicLibraryRepository.save(record);
        });
    }
}
