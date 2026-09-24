package com.smartparking.security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // API identity comes only from a valid Bearer token.
        // Ignore any OAuth browser session authentication.
        if (request.getRequestURI().startsWith("/api/")) {
            SecurityContextHolder.clearContext();
        }

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authHeader.substring(7);

        try {

            String email =
                    jwtService.extractEmail(token);

            if (email != null) {

                User user =
                        userRepository
                                .findByEmail(
                                        email.trim()
                                                .toLowerCase(
                                                        Locale.ROOT
                                                )
                                )
                                .orElse(null);

                if (user != null
                        && user.isEnabled()
                        && jwtService.isTokenValid(
                                token,
                                user.getEmail()
                        )) {

                    String role =
                            normalizeRole(
                                    user.getRole()
                            );

                    SimpleGrantedAuthority authority =
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),
                                    null,
                                    List.of(authority)
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );
                }
            }

        } catch (Exception ignored) {
            // Invalid/expired JWT remains unauthenticated.
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private String normalizeRole(
            String role) {

        if (role == null
                || role.isBlank()) {

            return "USER";
        }

        String normalized =
                role.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (normalized.startsWith("ROLE_")) {

            normalized =
                    normalized.substring(5);
        }

        return "ADMIN".equals(normalized)
                ? "ADMIN"
                : "USER";
    }
}