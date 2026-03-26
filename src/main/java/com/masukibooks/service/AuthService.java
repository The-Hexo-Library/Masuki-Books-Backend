package com.masukibooks.service;

import com.masukibooks.dto.request.LoginRequest;
import com.masukibooks.dto.request.RegisterRequest;
import com.masukibooks.dto.response.AuthResponse;
import com.masukibooks.entity.User;
import com.masukibooks.entity.UserRole;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.UserRepository;
import com.masukibooks.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }
        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .profession(request.getProfession())
                .preferredLanguage(request.getPreferredLanguage())
                .piiConsent(request.isPiiConsent())
                .piiConsentDate(request.isPiiConsent() ? LocalDateTime.now() : null)
                .status("active")
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = normalizeIdentifier(request.getIdentifier());
        Optional<LoginPrincipal> adminPrincipal = findAdminPrincipal(identifier);
        if (adminPrincipal.isPresent()) {
            LoginPrincipal admin = adminPrincipal.get();

            if (!passwordEncoder.matches(request.getPassword(), admin.passwordHash())) {
                throw new BusinessException("Invalid credentials");
            }
            if (!"active".equalsIgnoreCase(admin.status())) {
                throw new BusinessException("Account is " + admin.status());
            }

            String token = jwtTokenProvider.generateToken(admin.id(), admin.email(), UserRole.ADMIN.name());
            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(admin.id())
                    .email(admin.email())
                    .firstName(admin.firstName())
                    .lastName(admin.lastName())
                    .role(UserRole.ADMIN.name())
                    .build();
        }

        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("Account is " + user.getStatus());
        }

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    private Optional<LoginPrincipal> findAdminPrincipal(String identifier) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT *
                    FROM public.admin_users
                    WHERE lower(email) = lower(?)
                    LIMIT 1
                    """,
                    identifier
            );

            if (rows.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> row = rows.get(0);
            UUID id = extractUuid(row, "admin_id", "user_id", "admin_user_id", "id");
            String email = extractString(row, "email");
            String passwordHash = extractString(row, "password_hash", "password");
            String firstName = extractString(row, "first_name", "name");
            String lastName = extractString(row, "last_name");
            String status = extractStatus(row);

            if (id == null || email == null || passwordHash == null) {
                return Optional.empty();
            }

            return Optional.of(new LoginPrincipal(
                    id,
                    email,
                    passwordHash,
                    firstName == null ? "Admin" : firstName,
                    lastName == null ? "User" : lastName,
                    status == null ? "active" : status
            ));
        } catch (DataAccessException ex) {
            // If admin_users is not present in this environment, continue with users table login.
            return Optional.empty();
        }
    }

    private UUID extractUuid(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof UUID uuid) {
                return uuid;
            }
            if (value instanceof String str && !str.isBlank()) {
                return UUID.fromString(str);
            }
        }
        return null;
    }

    private String extractString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return null;
    }

    private String extractStatus(Map<String, Object> row) {
        String status = extractString(row, "status");
        if (status != null) {
            return status;
        }

        Object activeValue = row.get("is_active");
        if (activeValue instanceof Boolean active) {
            return active ? "active" : "inactive";
        }

        return "active";
    }

    private record LoginPrincipal(
            UUID id,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String status
    ) {
    }

    private String normalizeIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim();
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        Optional<User> byRepo = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier));
        if (byRepo.isPresent()) {
            return byRepo;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT user_id
                    FROM public.users
                    WHERE lower(email) = lower(?) OR phone_number = ?
                    LIMIT 1
                    """,
                    identifier,
                    identifier
            );

            if (rows.isEmpty()) {
                return Optional.empty();
            }

            UUID userId = extractUuid(rows.get(0), "user_id", "id");
            if (userId == null) {
                return Optional.empty();
            }

            return userRepository.findById(userId);
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    public AuthResponse adminLogin(LoginRequest request) {
        String identifier = normalizeIdentifier(request.getIdentifier());

        Optional<LoginPrincipal> adminPrincipal = findAdminPrincipal(identifier);
        if (adminPrincipal.isPresent()) {
            LoginPrincipal admin = adminPrincipal.get();

            if (!passwordEncoder.matches(request.getPassword(), admin.passwordHash())) {
                throw new BusinessException("Invalid credentials");
            }
            if (!"active".equalsIgnoreCase(admin.status())) {
                throw new BusinessException("Account is " + admin.status());
            }

            String token = jwtTokenProvider.generateToken(admin.id(), admin.email(), UserRole.ADMIN.name());
            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(admin.id())
                    .email(admin.email())
                    .firstName(admin.firstName())
                    .lastName(admin.lastName())
                    .role(UserRole.ADMIN.name())
                    .build();
        }

        User admin = findUserByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }
        if (admin.getRole() != UserRole.ADMIN) {
            throw new BusinessException("User is not an admin");
        }
        if (!"active".equals(admin.getStatus())) {
            throw new BusinessException("Admin account is inactive");
        }

        String token = jwtTokenProvider.generateToken(admin.getUserId(), admin.getEmail(), admin.getRole().name());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(admin.getUserId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .role(admin.getRole().name())
                .build();
    }
}
