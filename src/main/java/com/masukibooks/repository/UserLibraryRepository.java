package com.masukibooks.repository;

import com.masukibooks.entity.UserLibrary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLibraryRepository extends JpaRepository<UserLibrary, UUID> {

    Page<UserLibrary> findByUserUserId(UUID userId, Pageable pageable);

    Page<UserLibrary> findByUserUserIdAndStatus(UUID userId, String status, Pageable pageable);

    Page<UserLibrary> findByUserUserIdAndAccessType(UUID userId, String accessType, Pageable pageable);

    Page<UserLibrary> findByUserUserIdAndStatusAndAccessType(UUID userId, String status, String accessType, Pageable pageable);

    Optional<UserLibrary> findByUserUserIdAndProductProductId(UUID userId, UUID productId);

    boolean existsByUserUserIdAndProductProductIdAndStatusIn(UUID userId, UUID productId, List<String> statuses);

    long countByUserUserIdAndStatus(UUID userId, String status);

    void deleteByProductProductId(UUID productId);
}
