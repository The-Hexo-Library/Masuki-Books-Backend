package com.masukibooks.repository;

import com.masukibooks.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Page<SupportTicket> findByUserUserId(UUID userId, Pageable pageable);

    Page<SupportTicket> findByStatus(String status, Pageable pageable);

    Page<SupportTicket> findByUserUserIdAndStatus(UUID userId, String status, Pageable pageable);

    Page<SupportTicket> findByAssignedToAdminId(UUID adminId, Pageable pageable);

    long countByStatus(String status);
}
