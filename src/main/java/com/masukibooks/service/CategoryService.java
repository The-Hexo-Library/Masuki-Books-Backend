package com.masukibooks.service;

import com.masukibooks.entity.Category;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

        // Create corresponding folder in S3 bucket
        bookStorageService.createCategoryFolder(saved.getName());

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
            bookStorageService.createCategoryFolder(updates.getName());
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

}
