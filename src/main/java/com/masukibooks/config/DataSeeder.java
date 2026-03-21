package com.masukibooks.config;

import com.masukibooks.entity.*;
import com.masukibooks.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL    = "admin@masukibooks.com";
    private static final String ADMIN_PASSWORD = "Admin@2024!";
    private static final String ADMIN_FIRST    = "Masuki";
    private static final String ADMIN_LAST     = "Admin";
    private static final String ADMIN_ROLE     = "admin";

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDefaultCategories();
        seedSampleBooks();
    }

    private void seedAdmin() {
        AdminUser admin = adminUserRepository.findByEmail(ADMIN_EMAIL).orElse(null);

        if (admin == null) {
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
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setIsActive(true);
            admin.setRole(ADMIN_ROLE);
            adminUserRepository.save(admin);
            log.info("✅ Admin account password reset: {}", ADMIN_EMAIL);
        }
    }

    private void seedDefaultCategories() {
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
            Category existing = categoryRepository.findBySlug(c[1]).orElse(null);
            if (existing != null) {
                // Fix any null isActive or displayOrder from previous buggy seeds
                if (existing.getIsActive() == null) existing.setIsActive(true);
                if (existing.getDisplayOrder() == null) existing.setDisplayOrder(order);
                categoryRepository.save(existing);
            } else {
                Category cat = Category.builder()
                        .name(c[0])
                        .slug(c[1])
                        .description(c[2])
                        .displayOrder(order)
                        .isActive(true)
                        .build();
                categoryRepository.save(cat);
            }
            order++;
        }
        log.info("✅ Ensured {} default categories exist.", cats.length);
    }

    private void seedSampleBooks() {
        if (productRepository.count() > 0) {
            return;
        }

        Category business = categoryRepository.findBySlug("business").orElse(null);
        Category selfGrowth = categoryRepository.findBySlug("self-growth").orElse(null);
        Category technology = categoryRepository.findBySlug("technology").orElse(null);

        if (business == null || selfGrowth == null || technology == null) {
            log.warn("⚠️ Categories not found, skipping sample book seeding");
            return;
        }

        AdminUser admin = adminUserRepository.findByEmail(ADMIN_EMAIL).orElse(null);

        // Sample books matching the PDFs in /books folder
        Object[][] books = {
                {"8 Phases of Employee Burnout", "Dr. Sarah Mitchell", business,
                        "A comprehensive guide to identifying and preventing employee burnout across eight distinct phases.",
                        "MBK-BURN-001", "978-1-00001-001-0", new BigDecimal("299.00"), 180, "ebook", "pdf", true},
                {"Build a Thriving Culture", "James Whitfield", business,
                        "Strategies to build and sustain an energized, productive workplace culture.",
                        "MBK-CULT-002", "978-1-00001-002-7", new BigDecimal("349.00"), 220, "ebook", "pdf", true},
                {"Culture Built to Last", "Amanda Rodriguez", business,
                        "How to create organisational culture that stands the test of time and change.",
                        "MBK-CLST-003", "978-1-00001-003-4", new BigDecimal("399.00"), 250, "ebook", "pdf", true},
                {"Culture of Startup Innovation", "Kevin Chen", technology,
                        "Exploring innovation-driven culture in the modern startup ecosystem.",
                        "MBK-SINN-004", "978-1-00001-004-1", new BigDecimal("279.00"), 195, "ebook", "pdf", true},
                {"Culture vs Perks", "Lisa Nakamura", business,
                        "Why authentic culture matters more than ping-pong tables and free snacks.",
                        "MBK-CPRK-005", "978-1-00001-005-8", new BigDecimal("249.00"), 160, "ebook", "pdf", true},
                {"NEGO-SYNTHESIS 2050", "Dr. Alan Reeves", technology,
                        "A futuristic exploration of negotiation strategies powered by AI and behavioral science.",
                        "MBK-NEGO-006", "978-1-00001-006-5", new BigDecimal("449.00"), 300, "ebook", "pdf", true},
                {"Strategic Leadership for AI", "Prof. Maya Sharma", technology,
                        "A leader's guide to navigating the artificial intelligence revolution.",
                        "MBK-SLAI-007", "978-1-00001-007-2", new BigDecimal("499.00"), 280, "ebook", "pdf", true},
                {"TERRAFORMING THE TALENTSCAPE", "Robert Kim", business,
                        "Reshaping how organisations attract, develop, and retain talent in the 21st century.",
                        "MBK-TTLS-008", "978-1-00001-008-9", new BigDecimal("379.00"), 240, "ebook", "pdf", true},
                {"The Power of Pause", "Dr. Ellen Brooks", selfGrowth,
                        "Discover the transformative power of slowing down in a fast-paced world.",
                        "MBK-PAUS-009", "978-1-00001-009-6", new BigDecimal("199.00"), 150, "ebook", "pdf", true},
                {"The Synthetic Firm", "Dr. Thomas Grant", technology,
                        "How AI-driven companies are redefining the modern enterprise.",
                        "MBK-SYNF-010", "978-1-00001-010-2", new BigDecimal("429.00"), 270, "ebook", "pdf", true},
                {"Trust @Workplace", "Priya Venkatesh", selfGrowth,
                        "Building and sustaining trust as the foundation of high-performing teams.",
                        "MBK-TRST-011", "978-1-00001-011-9", new BigDecimal("259.00"), 175, "ebook", "pdf", true},
        };

        for (Object[] b : books) {
            Product product = Product.builder()
                    .title((String) b[0])
                    .author((String) b[1])
                    .category((Category) b[2])
                    .description((String) b[3])
                    .sku((String) b[4])
                    .isbn((String) b[5])
                    .price((BigDecimal) b[6])
                    .pages((Integer) b[7])
                    .format((String) b[8])
                    .language("en")
                    .contentType("digital")
                    .fileFormat((String) b[9])
                    .downloadable((Boolean) b[10])
                    .totalPages((Integer) b[7])
                    .previewPages(10)
                    .maxDownloads(3)
                    .status("active")
                    .publisher("Masuki Books Publishing")
                    .publicationDate(LocalDate.of(2024, 6, 15))
                    .createdBy(admin)
                    .build();

            product = productRepository.save(product);

            // Create inventory entry
            Inventory inv = Inventory.builder()
                    .product(product)
                    .quantity(999)
                    .lowStockThreshold(10)
                    .build();
            inventoryRepository.save(inv);
        }

        log.info("✅ Seeded {} sample books.", books.length);
    }
}
