package com.masukibooks.service;

import com.masukibooks.entity.Category;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

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
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(UUID categoryId, Category updates) {
        Category category = getCategory(categoryId);
        if (updates.getName() != null)
            category.setName(updates.getName());
        if (updates.getSlug() != null)
            category.setSlug(updates.getSlug());
        if (updates.getDescription() != null)
            category.setDescription(updates.getDescription());
        // if (updates.getImageUrl() != null)
        // category.setImageUrl(updates.getImageUrl());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = getCategory(categoryId);
        List<Category> children = categoryRepository.findByParentCategoryCategoryIdAndIsActiveTrue(categoryId);
        if (!children.isEmpty()) {
            throw new BusinessException("Cannot delete category with subcategories");
        }
        categoryRepository.delete(category);
    }

}
