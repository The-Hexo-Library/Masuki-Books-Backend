package com.masukibooks.service;

import com.masukibooks.entity.Category;
import com.masukibooks.dto.response.CategoryResponse;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookStorageService bookStorageService;

    public List<Category> getRootCategories() {
        List<Category> roots = categoryRepository.findByParentCategoryIsNull();
        // Fallback: if no root categories found, return all categories
        if (roots.isEmpty()) {
            return categoryRepository.findAll();
        }
        return roots;
    }

    public List<Category> getRootCategoriesWithBooks() {
        List<Category> roots = getRootCategories();
        Map<UUID, Long> countMap = new HashMap<>();
        for (Object[] row : categoryRepository.countBooksByCategory()) {
            UUID catId = (UUID) row[0];
            Long count = (Long) row[1];
            countMap.put(catId, count);
        }

        return roots.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> countMap.getOrDefault(c.getCategoryId(), 0L) > 0)
                .sorted(Comparator.comparingInt(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 0))
                .collect(Collectors.toList());
    }

    public List<Category> getChildCategories(UUID parentId) {
        return categoryRepository.findByParentCategoryCategoryIdAndIsActiveTrue(parentId);
    }

    public Category getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Transactional
    public Category createCategory(Category category) {
        if (category.getParentCategory() != null &&
                category.getParentCategory().getCategoryId() != null) {
            Category parent = getCategory(category.getParentCategory().getCategoryId());
            category.setParentCategory(parent);
        }
        Category saved = categoryRepository.save(category);

        return saved;
    }

    @Transactional
    public Category updateCategory(UUID categoryId, Category updates) {
        Category category = getCategory(categoryId);
        String oldName = category.getName();

        if (updates.getName() != null)
            category.setName(updates.getName());
        if (updates.getSlug() != null)
            category.setSlug(updates.getSlug());
        if (updates.getDescription() != null)
            category.setDescription(updates.getDescription());
        // if (updates.getImageUrl() != null)
        // category.setImageUrl(updates.getImageUrl());

        Category saved = categoryRepository.save(category);

        // If category name changed, delete old folder and create new one
        if (updates.getName() != null && !updates.getName().equals(oldName)) {
            log.info("Category renamed from '{}' to '{}' — updating S3 folder", oldName, updates.getName());
            bookStorageService.deleteCategoryFolder(oldName);
        }

        return saved;
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = getCategory(categoryId);
        List<Category> children = categoryRepository.findByParentCategoryCategoryIdAndIsActiveTrue(categoryId);
        if (!children.isEmpty()) {
            throw new BusinessException("Cannot delete category with subcategories");
        }

        // Delete the corresponding S3 folder and all its contents
        bookStorageService.deleteCategoryFolder(category.getName());

        categoryRepository.delete(category);
    }

    public List<CategoryResponse> getCategoriesWithBookCount() {
        List<Category> categories = getRootCategories();
        Map<UUID, Long> countMap = new HashMap<>();
        for (Object[] row : categoryRepository.countBooksByCategory()) {
            UUID catId = (UUID) row[0];
            Long count = (Long) row[1];
            countMap.put(catId, count);
        }
        return categories.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .sorted(Comparator.comparingInt(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 0))
                .map(c -> CategoryResponse.builder()
                        .categoryId(c.getCategoryId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .description(c.getDescription())
                        .displayOrder(c.getDisplayOrder())
                        .isActive(c.getIsActive())
                        .bookCount(countMap.getOrDefault(c.getCategoryId(), 0L))
                        .build())
                .collect(Collectors.toList());
    }

}
