package com.masukibooks.security;

import com.masukibooks.entity.AdminUser;
import com.masukibooks.entity.User;
import com.masukibooks.repository.AdminUserRepository;
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
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            UUID userId = tokenProvider.getUserIdFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);

            Object principal = null;
            List<SimpleGrantedAuthority> authorities;

            if (role != null && (role.contains("admin") || role.contains("moderator"))) {
                Optional<AdminUser> admin = adminUserRepository.findById(userId);
                if (admin.isPresent()) {
                    principal = admin.get();
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                } else {
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                Optional<User> user = userRepository.findById(userId);
                if (user.isPresent()) {
                    principal = user.get();
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                } else {
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, 
                        ((List<SimpleGrantedAuthority>) 
                        (principal instanceof AdminUser 
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            : List.of(new SimpleGrantedAuthority("ROLE_USER")))));
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
