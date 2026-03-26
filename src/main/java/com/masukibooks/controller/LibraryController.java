package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.LibraryResponse;
import com.masukibooks.dto.response.PublicLibraryResponse;
import com.masukibooks.entity.User;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.service.PublicLibraryService;
import com.masukibooks.service.SubscriptionService;
import com.masukibooks.service.UserLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final PublicLibraryService publicLibraryService;
    private final UserLibraryService userLibraryService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PublicLibraryResponse>>> publicLibrary() {
        return ResponseEntity.ok(ApiResponse.success("Public library retrieved", publicLibraryService.listPublicItems()));
    }

    @GetMapping("/private")
    public ResponseEntity<ApiResponse<Page<LibraryResponse>>> privateLibrary(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        if (!subscriptionService.isSubscriptionActive(user.getUserId())) {
            throw new BusinessException("Subscribe to access your private library");
        }
        return ResponseEntity.ok(ApiResponse.success("Private library retrieved",
                userLibraryService.getUserLibrary(user.getUserId(), null, null, pageable)));
    }
}
