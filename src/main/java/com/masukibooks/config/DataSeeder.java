package com.masukibooks.config;

import com.masukibooks.entity.AdminUser;
import com.masukibooks.entity.Category;
import com.masukibooks.repository.AdminUserRepository;
import com.masukibooks.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Admin credentials ─────────────────────────────────────
    private static final String ADMIN_EMAIL    = "admin@masukibooks.com";
    private static final String ADMIN_PASSWORD = "Admin@2024!";
    private static final String ADMIN_FIRST    = "Masuki";
    private static final String ADMIN_LAST     = "Admin";
    private static final String ADMIN_ROLE     = "admin";

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDefaultCategories();
    }

    // ── Seed the superadmin account ───────────────────────────
    private void seedAdmin() {
        AdminUser admin = adminUserRepository.findByEmail(ADMIN_EMAIL).orElse(null);

        if (admin == null) {
            // Create new admin account
            admin = AdminUser.builder()
                    .email(ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                    .firstName(ADMIN_FIRST)
                    .lastName(ADMIN_LAST)
                    .role(ADMIN_ROLE)
                    .isActive(true)
                    .build();
            adminUserRepository.save(admin);
            log.info("✅ Admin account created: {}", ADMIN_EMAIL);
        } else {
            // Reset password and ensure account is active (handles stale/wrong hash)
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setIsActive(true);
            admin.setRole(ADMIN_ROLE);
            adminUserRepository.save(admin);
            log.info("✅ Admin account password reset: {}", ADMIN_EMAIL);
        }
    }

    // ── Seed default book categories (only if none exist) ─────
    private void seedDefaultCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        String[][] cats = {
                {"Fiction",           "fiction",           "Novels, short stories, and literary works"},
                {"Non-Fiction",       "non-fiction",       "Biographies, essays, and factual writing"},
                {"Self Growth",       "self-growth",       "Personal development and self-improvement"},
                {"Science",           "science",           "Natural sciences, physics, biology, and more"},
                {"Technology",        "technology",        "Programming, AI, engineering, and tech trends"},
                {"History",           "history",           "World history, civilisations, and events"},
                {"Business",          "business",          "Entrepreneurship, finance, and management"},
                {"Children",          "children",          "Picture books and young-reader stories"},
                {"Comics & Manga",    "comics-manga",      "Graphic novels, comics, and manga series"},
                {"Religion & Spirituality", "religion",    "Philosophy, faith, and spiritual exploration"},
        };

        int order = 1;
        for (String[] c : cats) {
            Category cat = Category.builder()
                    .name(c[0])
                    .slug(c[1])
                    .description(c[2])
                    .displayOrder(order++)
                    .isActive(true)
                    .build();
            categoryRepository.save(cat);
        }
        log.info("✅ Seeded {} default categories.", cats.length);
    }
}
