package com.smartparking.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService =
                jwtService;

        this.userRepository =
                userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        OAuth2User oauthUser =
                (OAuth2User)
                        authentication
                                .getPrincipal();

        String email =
                oauthUser.getAttribute(
                        "email"
                );

        if (email == null
                || email.isBlank()) {

            redirectError(
                    response,
                    "google_email_missing"
            );

            return;
        }

        String normalizedEmail =
                email.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        User user =
                userRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElse(null);

        if (user == null) {

            redirectError(
                    response,
                    "google_user_not_found"
            );

            return;
        }

        if (!user.isEnabled()) {

            redirectError(
                    response,
                    "account_disabled"
            );

            return;
        }

        String role =
                normalizeRole(
                        user.getRole()
                );

        /*
         * Public customer Google login is USER-only.
         * ADMIN must use the separate admin authentication flow.
         */
        if ("ADMIN".equals(role)) {
            redirectError(
                    response,
                    "invalid_credentials"
            );
            return;
        }
        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        "USER"
                );

        String encodedToken =
                URLEncoder.encode(
                        token,
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(
                frontendUrl
                        + "/oauth-success?token="
                        + encodedToken
        );
    }

    private void redirectError(
            HttpServletResponse response,
            String error)
            throws IOException {

        String encodedError =
                URLEncoder.encode(
                        error,
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(
                frontendUrl
                        + "/login?error="
                        + encodedError
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

        if (normalized.startsWith(
                "ROLE_")) {

            normalized =
                    normalized.substring(5);
        }

        return "ADMIN".equals(
                normalized)
                ? "ADMIN"
                : "USER";
    }
}