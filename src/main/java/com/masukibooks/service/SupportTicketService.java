package com.masukibooks.service;

import com.masukibooks.dto.request.SupportTicketRequest;
import com.masukibooks.dto.response.SupportTicketResponse;
import com.masukibooks.entity.*;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.AdminUserRepository;
import com.masukibooks.repository.OrderRepository;
import com.masukibooks.repository.SupportTicketRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public SupportTicketResponse createTicket(UUID userId, SupportTicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .status("open")
                .build();

        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            ticket.setRelatedOrder(order);
        }

        return toResponse(ticketRepository.save(ticket));
    }

    public Page<SupportTicketResponse> getUserTickets(UUID userId, String status, Pageable pageable) {
        Page<SupportTicket> tickets;
        if (status != null && !status.isBlank()) {
            tickets = ticketRepository.findByUserUserIdAndStatus(userId, status, pageable);
        } else {
            tickets = ticketRepository.findByUserUserId(userId, pageable);
        }
        return tickets.map(this::toResponse);
    }

    public SupportTicketResponse getTicket(UUID ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        return toResponse(ticket);
    }

    public Page<SupportTicketResponse> getAllTickets(String status, Pageable pageable) {
        Page<SupportTicket> tickets;
        if (status != null && !status.isBlank()) {
            tickets = ticketRepository.findByStatus(status, pageable);
        } else {
            tickets = ticketRepository.findAll(pageable);
        }
        return tickets.map(this::toResponse);
    }

    @Transactional
    public SupportTicketResponse respondToTicket(UUID ticketId, String response, String newStatus, UUID adminId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        if ("closed".equals(ticket.getStatus()) || "resolved".equals(ticket.getStatus())) {
            throw new BusinessException("Cannot update a " + ticket.getStatus() + " ticket");
        }

        if (response != null && !response.isBlank()) {
            ticket.setAdminResponse(response);
        }

        if (newStatus != null && !newStatus.isBlank()) {
            ticket.setStatus(newStatus);
            if ("resolved".equals(newStatus) || "closed".equals(newStatus)) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        if (adminId != null) {
            AdminUser admin = adminUserRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
            ticket.setAssignedTo(admin);
        }

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public SupportTicketResponse assignTicket(UUID ticketId, UUID adminId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        ticket.setAssignedTo(admin);
        if ("open".equals(ticket.getStatus())) {
            ticket.setStatus("in_progress");
        }
        return toResponse(ticketRepository.save(ticket));
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        SupportTicketResponse.SupportTicketResponseBuilder builder = SupportTicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUser().getUserId())
                .userEmail(ticket.getUser().getEmail())
                .userName(ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .adminResponse(ticket.getAdminResponse())
                .resolvedAt(ticket.getResolvedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt());

        if (ticket.getAssignedTo() != null) {
            builder.assignedToId(ticket.getAssignedTo().getAdminId())
                   .assignedToName(ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName());
        }
        if (ticket.getRelatedOrder() != null) {
            builder.orderId(ticket.getRelatedOrder().getOrderId());
        }

        return builder.build();
    }
}
