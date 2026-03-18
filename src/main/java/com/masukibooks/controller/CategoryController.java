package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.CategoryResponse;
import com.masukibooks.entity.Category;
import com.masukibooks.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        List<CategoryResponse> dtos = categoryService.getRootCategories().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", dtos));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Category retrieved", toResponse(categoryService.getCategory(categoryId))));
    }

    @GetMapping("/{categoryId}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(@PathVariable UUID categoryId) {
        List<CategoryResponse> dtos = categoryService.getChildCategories(categoryId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Sub-categories retrieved", dtos));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.success("Category created", toResponse(categoryService.createCategory(category))));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable UUID categoryId,
                                                                @RequestBody Category updates) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", toResponse(categoryService.updateCategory(categoryId, updates))));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .displayOrder(c.getDisplayOrder())
                .build();
    }
}
