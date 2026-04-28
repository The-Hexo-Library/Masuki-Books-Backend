package com.masukibooks.repository;

import com.masukibooks.entity.ContactSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactSubmissionRepository extends JpaRepository<ContactSubmission, UUID> {
    List<ContactSubmission> findAllByOrderByCreatedAtDesc();
    List<ContactSubmission> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
