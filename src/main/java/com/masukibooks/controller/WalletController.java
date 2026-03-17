package com.masukibooks.controller;

import com.masukibooks.dto.request.TopUpRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.WalletResponse;
import com.masukibooks.dto.response.WalletTransactionResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(user.getUserId())));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<WalletResponse>> topUp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up successfully",
                walletService.topUp(user.getUserId(), request.getAmount())));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.getTransactions(user.getUserId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}
