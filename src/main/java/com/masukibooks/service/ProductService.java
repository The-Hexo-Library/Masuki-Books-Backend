package com.masukibooks.service;

import com.masukibooks.dto.request.ProductRequest;
import com.masukibooks.dto.response.ProductResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;

    public Page<ProductResponse> searchProducts(String keyword, UUID categoryId,
                                                String language, BigDecimal minPrice,
                                                BigDecimal maxPrice, Pageable pageable) {
        return productRepository.searchProducts(keyword, categoryId, language,
                minPrice, maxPrice, pageable).map(this::toResponse);
    }

    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(UUID.fromString(request.getCategoryId()))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
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

        product = productRepository.save(product);

        // Only create inventory for physical products
        boolean isDigital = "digital".equals(product.getContentType());
        if (!isDigital) {
            Inventory inventory = Inventory.builder()
                    .product(product)
                    .quantity(0)
                    .lowStockThreshold(5)
                    .build();
            inventoryRepository.save(inventory);
        }

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(UUID.fromString(request.getCategoryId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }
        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getAuthor() != null) product.setAuthor(request.getAuthor());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getLanguage() != null) product.setLanguage(request.getLanguage());
        if (request.getStatus() != null) product.setStatus(request.getStatus());

        // Digital content fields
        if (request.getContentType() != null) product.setContentType(request.getContentType());
        if (request.getFileFormat() != null) product.setFileFormat(request.getFileFormat());
        if (request.getFileSizeBytes() != null) product.setFileSizeBytes(request.getFileSizeBytes());
        if (request.getTotalPages() != null) product.setTotalPages(request.getTotalPages());
        if (request.getPreviewPages() != null) product.setPreviewPages(request.getPreviewPages());
        if (request.getDownloadable() != null) product.setDownloadable(request.getDownloadable());
        if (request.getMaxDownloads() != null) product.setMaxDownloads(request.getMaxDownloads());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setStatus("inactive");
        productRepository.save(product);
    }

    @Transactional
    public ProductImage addProductImage(UUID productId, String imageUrl, boolean isPrimary, int sortOrder) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductImage image = ProductImage.builder()
                .product(product)
                .url(imageUrl)
                .isPrimary(isPrimary)
                .displayOrder(sortOrder)
                .build();
        return productImageRepository.save(image);
    }

    private ProductResponse toResponse(Product p) {
        boolean isDigital = "digital".equals(p.getContentType()) || "both".equals(p.getContentType());
        Inventory inv = inventoryRepository.findByProductProductId(p.getProductId()).orElse(null);
        int stock = inv != null ? inv.getQuantity() : 0;
        Double avgRating = reviewRepository.getAverageRatingByProductId(p.getProductId());
        List<String> imageUrls = p.getImages() != null
                ? p.getImages().stream().map(ProductImage::getUrl).collect(Collectors.toList())
                : List.of();

        return ProductResponse.builder()
                .productId(p.getProductId())
                .isbn(p.getIsbn())
                .title(p.getTitle())
                .description(p.getDescription())
                .author(p.getAuthor())
                .publisher(p.getPublisher())
                .language(p.getLanguage())
                .format(p.getFormat())
                .pages(p.getPages())
                .publicationDate(p.getPublicationDate())
                .price(p.getPrice())
                .status(p.getStatus())
                .stockQuantity(isDigital ? null : stock)
                .inStock(isDigital || stock > 0)
                .averageRating(avgRating)
                .imageUrls(imageUrls)
                .contentType(p.getContentType())
                .fileFormat(p.getFileFormat())
                .totalPages(p.getTotalPages())
                .previewPages(p.getPreviewPages())
                .downloadable(Boolean.TRUE.equals(p.getDownloadable()))
                .build();
    }
}
