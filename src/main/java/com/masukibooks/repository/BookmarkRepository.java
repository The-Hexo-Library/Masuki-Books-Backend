package com.masukibooks.repository;

import com.masukibooks.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    List<Bookmark> findByUserUserIdAndProductProductIdOrderByPageNumberAsc(UUID userId, UUID productId);

    Optional<Bookmark> findByUserUserIdAndProductProductIdAndPageNumber(UUID userId, UUID productId, Integer pageNumber);

    Page<Bookmark> findByUserUserId(UUID userId, Pageable pageable);
}
