package com.masukibooks.controller;

import com.masukibooks.dto.request.SupportTicketRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.SupportTicketResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SupportTicketRequest request) {
        SupportTicketResponse response = supportTicketService.createTicket(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Support ticket created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> getMyTickets(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SupportTicketResponse> tickets = supportTicketService.getUserTickets(
                user.getUserId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved", tickets));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicket(
            @PathVariable UUID ticketId) {
        SupportTicketResponse ticket = supportTicketService.getTicket(ticketId);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved", ticket));
    }
}
