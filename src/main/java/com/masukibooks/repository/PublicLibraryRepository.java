package com.masukibooks.repository;

import com.masukibooks.entity.PublicLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicLibraryRepository extends JpaRepository<PublicLibrary, UUID> {
    List<PublicLibrary> findByVisibilityOrderByCreatedAtDesc(String visibility);
    List<PublicLibrary> findByVisibilityIgnoreCaseOrderByCreatedAtDesc(String visibility);
    Optional<PublicLibrary> findByProductProductId(UUID productId);
    long countByVisibility(String visibility);
    void deleteByProductProductId(UUID productId);
}
