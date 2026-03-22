package com.masukibooks.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap-admin.email:admin@masukibooks.com}")
    private String email;

    @Value("${app.bootstrap-admin.password:Admin@2024!}")
    private String password;

    @Value("${app.bootstrap-admin.first-name:Super}")
    private String firstName;

    @Value("${app.bootstrap-admin.last-name:Admin}")
    private String lastName;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank()) {
            log.warn("Admin bootstrap skipped because email is blank.");
            return;
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM public.admin_users WHERE lower(email) = lower(?)",
                Integer.class,
                normalizedEmail
        );

        if (existing != null && existing > 0) {
            log.info("Admin bootstrap: account already exists for {}", normalizedEmail);
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO public.admin_users
                    (admin_id, email, password_hash, first_name, last_name, role, is_active, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, true, now(), now())
                """,
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(password),
                firstName,
                lastName,
                "ADMIN"
        );

        log.info("Admin bootstrap: created default admin account for {}", normalizedEmail);
    }
}
