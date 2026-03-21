package com.masukibooks.repository;

import com.masukibooks.entity.BookResale;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookResaleRepository extends JpaRepository<BookResale, UUID> {

    Page<BookResale> findByStatus(String status, Pageable pageable);

    Page<BookResale> findBySellerUserId(UUID sellerId, Pageable pageable);

    Optional<BookResale> findByUserLibraryUserLibraryIdAndStatus(UUID userLibraryId, String status);

    boolean existsByUserLibraryUserLibraryIdAndStatusIn(UUID userLibraryId, List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BookResale r WHERE r.resaleId = :id")
    Optional<BookResale> findByIdForUpdate(@Param("id") UUID id);
}
