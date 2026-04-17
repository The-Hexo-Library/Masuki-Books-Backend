package com.masukibooks.repository;

import com.masukibooks.entity.BooksMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface BooksMetadataRepository extends JpaRepository<BooksMetadata, UUID> {
    Optional<BooksMetadata> findBySku(String sku);
    Optional<BooksMetadata> findByIsbn(String isbn);

    Page<BooksMetadata> findByCategoryCategoryIdAndStatus(UUID categoryId, String status, Pageable pageable);

    @Query(value = """
        SELECT p.* FROM books_metadata p
        WHERE p.status = 'active'
        AND (CAST(:keyword AS text) IS NULL OR
            p.title ILIKE CONCAT('%', CAST(:keyword AS text), '%')
            OR p.author ILIKE CONCAT('%', CAST(:keyword AS text), '%')
            OR p.isbn LIKE CONCAT('%', CAST(:keyword AS text), '%'))
        AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
        AND (CAST(:language AS text) IS NULL OR p.language = CAST(:language AS text))
        AND (CAST(:minPrice AS numeric) IS NULL OR p.price >= CAST(:minPrice AS numeric))
        AND (CAST(:maxPrice AS numeric) IS NULL OR p.price <= CAST(:maxPrice AS numeric))
    """,
    countQuery = """
        SELECT count(*) FROM books_metadata p
        WHERE p.status = 'active'
        AND (CAST(:keyword AS text) IS NULL OR
            p.title ILIKE CONCAT('%', CAST(:keyword AS text), '%')
            OR p.author ILIKE CONCAT('%', CAST(:keyword AS text), '%')
            OR p.isbn LIKE CONCAT('%', CAST(:keyword AS text), '%'))
        AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
        AND (CAST(:language AS text) IS NULL OR p.language = CAST(:language AS text))
        AND (CAST(:minPrice AS numeric) IS NULL OR p.price >= CAST(:minPrice AS numeric))
        AND (CAST(:maxPrice AS numeric) IS NULL OR p.price <= CAST(:maxPrice AS numeric))
    """,
    nativeQuery = true)
    Page<BooksMetadata> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("language") String language,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    Page<BooksMetadata> findByStatus(String status, Pageable pageable);

}

