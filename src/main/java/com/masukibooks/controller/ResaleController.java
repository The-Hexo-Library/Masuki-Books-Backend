package com.masukibooks.controller;

import com.masukibooks.dto.request.ResaleListRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.ResaleListingResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.ResaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/resale")
@RequiredArgsConstructor
public class ResaleController {

        private final ResaleService resaleService;

        @GetMapping("/marketplace")
        public ResponseEntity<ApiResponse<Page<ResaleListingResponse>>> getMarketplace(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(ApiResponse.success(
                                resaleService.getMarketplace(
                                                PageRequest.of(page, size, Sort.by("listedAt").descending()))));
        }

        @GetMapping("/my-listings")
        public ResponseEntity<ApiResponse<Page<ResaleListingResponse>>> getMyListings(
                        @AuthenticationPrincipal User user,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(ApiResponse.success(
                                resaleService.getMyListings(user.getUserId(),
                                                PageRequest.of(page, size, Sort.by("listedAt").descending()))));
        }

        @PostMapping("/list")
        public ResponseEntity<ApiResponse<ResaleListingResponse>> listForResale(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody ResaleListRequest request) {
                return ResponseEntity.ok(ApiResponse.success("Book listed for resale",
                                resaleService.listForResale(user.getUserId(), request)));
        }

        @PostMapping("/{resaleId}/buy")
        public ResponseEntity<ApiResponse<ResaleListingResponse>> buyResale(
                        @AuthenticationPrincipal User user,
                        @PathVariable UUID resaleId) {
                return ResponseEntity.ok(ApiResponse.success("Book purchased successfully",
                                resaleService.buyResale(user.getUserId(), resaleId)));
        }

        @PostMapping("/{resaleId}/cancel")
        public ResponseEntity<ApiResponse<Void>> cancelListing(
                        @AuthenticationPrincipal User user,
                        @PathVariable UUID resaleId) {
                resaleService.cancelListing(user.getUserId(), resaleId);
                return ResponseEntity.ok(ApiResponse.success("Listing cancelled", null));
        }
}
