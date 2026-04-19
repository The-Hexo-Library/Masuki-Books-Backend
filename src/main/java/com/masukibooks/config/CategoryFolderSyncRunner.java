package com.masukibooks.config;

import com.masukibooks.entity.Category;
import com.masukibooks.repository.CategoryRepository;
import com.masukibooks.service.BookStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On application startup, ensures that every category in the database
 * has a corresponding folder in the S3 bucket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10) // run after AdminBootstrapRunner
public class CategoryFolderSyncRunner implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final BookStorageService bookStorageService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Category> categories = categoryRepository.findAll();
            if (categories.isEmpty()) {
                log.info("No categories found — skipping S3 folder sync.");
                return;
            }

            List<String> categoryNames = categories.stream()
                    .map(Category::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();

            log.info("Syncing {} category folders to S3 bucket on startup...", categoryNames.size());
            bookStorageService.syncCategoryFolders(categoryNames);
        } catch (Exception ex) {
            // Don't fail startup if S3 sync fails
            log.warn("Failed to sync category folders to S3 on startup: {}", ex.getMessage());
        }
    }
}
