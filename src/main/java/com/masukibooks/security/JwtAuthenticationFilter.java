package com.masukibooks.security;

import com.masukibooks.entity.User;
import com.masukibooks.entity.UserRole;
import com.masukibooks.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            UUID userId = tokenProvider.getUserIdFromToken(token);
            String tokenRole = tokenProvider.getRoleFromToken(token);
            Optional<User> user = userRepository.findById(userId);

            if (user.isEmpty() && "ADMIN".equalsIgnoreCase(tokenRole)) {
                User adminPrincipal = User.builder()
                        .userId(userId)
                        .email(tokenProvider.getEmailFromToken(token))
                        .firstName("Admin")
                        .lastName("User")
                        .status("active")
                        .role(UserRole.ADMIN)
                        .build();
                user = Optional.of(adminPrincipal);
            }

            if (user.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }
            User principal = user.get();
            String role = principal.getRole() != null ? principal.getRole().name() : tokenRole;
            if (!StringUtils.hasText(role)) {
                role = "USER";
            }
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
                    null,
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
