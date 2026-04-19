package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.CategoryResponse;
import com.masukibooks.dto.response.LibraryResponse;
import com.masukibooks.dto.response.PublicLibraryResponse;
import com.masukibooks.entity.User;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.service.CategoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LibraryController {

    private final PublicLibraryService publicLibraryService;
    private final UserLibraryService userLibraryService;
    private final SubscriptionService subscriptionService;
    private final CategoryService categoryService;

    @GetMapping("/library/public")
    public ResponseEntity<ApiResponse<List<PublicLibraryResponse>>> publicLibrary() {
        return ResponseEntity.ok(ApiResponse.success("Public library retrieved", publicLibraryService.listPublicItems()));
    }

    @GetMapping("/library/public/search")
    public ResponseEntity<ApiResponse<List<PublicLibraryResponse>>> searchPublicLibrary(
            @RequestParam(value = "q", defaultValue = "") String query) {
        List<PublicLibraryResponse> all = publicLibraryService.listPublicItems();
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Public library retrieved", all));
        }
        String q = query.trim().toLowerCase();
        List<PublicLibraryResponse> filtered = all.stream()
                .filter(item -> (item.getTitle() != null && item.getTitle().toLowerCase().contains(q))
                        || (item.getAuthor() != null && item.getAuthor().toLowerCase().contains(q))
                        || (item.getCategoryName() != null && item.getCategoryName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Search results", filtered));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> publicCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", categoryService.getCategoriesWithBookCount()));
    }

    @GetMapping("/library/private")
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
