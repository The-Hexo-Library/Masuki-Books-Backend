package com.masukibooks.repository;

import com.masukibooks.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findByIsbn(String isbn);

    Page<Product> findByCategoryCategoryIdAndStatus(UUID categoryId, String status, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = 'active'
        AND (:keyword IS NULL OR
            LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR p.isbn LIKE CONCAT('%', :keyword, '%'))
        AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
        AND (:language IS NULL OR p.language = :language)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
    """)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("language") String language,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    Page<Product> findByStatus(String status, Pageable pageable);
}

