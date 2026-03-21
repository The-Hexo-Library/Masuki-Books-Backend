package com.masukibooks.service;

import com.masukibooks.dto.request.LoginRequest;
import com.masukibooks.dto.request.RegisterRequest;
import com.masukibooks.dto.response.AuthResponse;
import com.masukibooks.entity.User;
import com.masukibooks.entity.AdminUser;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.AdminUserRepository;
import com.masukibooks.repository.UserRepository;
import com.masukibooks.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WalletService walletService;

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
                .build();

        user = userRepository.save(user);

        // Auto-create wallet for new user
        walletService.getOrCreateWallet(user.getUserId());

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), "user");
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role("user")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByPhoneNumber(request.getIdentifier()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("Account is " + user.getStatus());
        }

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), "user");
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role("user")
                .build();
    }

    public AuthResponse adminLogin(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByEmail(request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }
        if (!admin.getIsActive()) {
            throw new BusinessException("Admin account is inactive");
        }

        String token = jwtTokenProvider.generateToken(admin.getAdminId(), admin.getEmail(), admin.getRole());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(admin.getAdminId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .role(admin.getRole())
                .build();
    }
}
