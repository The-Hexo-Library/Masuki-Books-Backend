package com.masukibooks.config;

import com.masukibooks.entity.User;
import com.masukibooks.entity.UserRole;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
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

        User existing = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existing != null) {
            boolean changed = false;
            if (existing.getRole() != UserRole.ADMIN) {
                existing.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (!"active".equalsIgnoreCase(existing.getStatus())) {
                existing.setStatus("active");
                changed = true;
            }
            if (existing.getPasswordHash() == null || !passwordEncoder.matches(password, existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode(password));
                changed = true;
            }
            if (changed) {
                userRepository.save(existing);
                log.info("Admin bootstrap: updated admin account for {} in users table", normalizedEmail);
            } else {
                log.info("Admin bootstrap: account already active with ADMIN role for {}", normalizedEmail);
            }
            return;
        }

        User admin = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .preferredLanguage("en")
                .piiConsent(false)
                .emailVerified(true)
                .phoneVerified(false)
                .status("active")
                .role(UserRole.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Admin bootstrap: created default admin account for {} in users table", normalizedEmail);
    }
}
