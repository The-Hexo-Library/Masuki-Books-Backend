package com.masukibooks.repository;

import com.masukibooks.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentCategoryIsNullAndIsActiveTrue();
    List<Category> findByParentCategoryCategoryIdAndIsActiveTrue(UUID parentId);
    List<Category> findByParentCategoryIsNull();

    @Query("SELECT c.categoryId, COUNT(b.productId) FROM Category c LEFT JOIN c.products b GROUP BY c.categoryId")
    List<Object[]> countBooksByCategory();
}
