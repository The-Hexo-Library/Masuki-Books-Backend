package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<Category>>> getRootCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", categoryService.getRootCategories()));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Category>> getCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Category retrieved", categoryService.getCategory(categoryId)));
    }

    @GetMapping("/{categoryId}/children")
    public ResponseEntity<ApiResponse<List<Category>>> getChildren(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Sub-categories retrieved", categoryService.getChildCategories(categoryId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.success("Category created", categoryService.createCategory(category)));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable UUID categoryId,
                                                                @RequestBody Category updates) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.updateCategory(categoryId, updates)));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}
