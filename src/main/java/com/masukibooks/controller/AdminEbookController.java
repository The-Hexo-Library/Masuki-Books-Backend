package com.masukibooks.controller;

import com.masukibooks.dto.request.PublicLibraryRequest;
import com.masukibooks.dto.request.SubscriptionPlanRequest;
import com.masukibooks.dto.request.ProductRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.ProductResponse;
import com.masukibooks.dto.response.PublicLibraryResponse;
import com.masukibooks.dto.response.SubscriptionResponse;
import com.masukibooks.entity.Category;
import com.masukibooks.entity.User;
import com.masukibooks.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminEbookController {

    private final UserService userService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final PublicLibraryService publicLibraryService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> getUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.listUsers(pageable)));
    }

    @PostMapping("/books")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createBook(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book metadata created", productService.createProduct(request)));
    }

    @PutMapping("/books/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateBook(@PathVariable UUID bookId,
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book metadata updated", productService.updateProduct(bookId, request)));
    }

    @DeleteMapping("/books/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable UUID bookId) {
        productService.deleteProduct(bookId);
        return ResponseEntity.ok(ApiResponse.success("Book metadata deleted", null));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.success("Category created", categoryService.createCategory(category)));
    }

    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable UUID categoryId, @RequestBody Category updates) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.updateCategory(categoryId, updates)));
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    @GetMapping("/public-library")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PublicLibraryResponse>>> listPublicLibrary() {
        return ResponseEntity.ok(ApiResponse.success("Public library records retrieved", publicLibraryService.listPublicItems()));
    }

    @PostMapping("/public-library")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PublicLibraryResponse>> upsertPublicLibrary(@Valid @RequestBody PublicLibraryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Public library record saved", publicLibraryService.createOrUpdate(request)));
    }

    @DeleteMapping("/public-library/{publicLibraryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePublicLibrary(@PathVariable UUID publicLibraryId) {
        publicLibraryService.delete(publicLibraryId);
        return ResponseEntity.ok(ApiResponse.success("Public library record deleted", null));
    }

    @PostMapping("/subscriptions/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscriptionPlan(
            @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription plan created", subscriptionService.createPlan(request)));
    }
}
