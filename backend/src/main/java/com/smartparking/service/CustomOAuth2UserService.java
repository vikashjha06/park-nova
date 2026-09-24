package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;

@Service
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(
            UserRepository userRepository) {

        this.userRepository =
                userRepository;
    }

    @Override
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauthUser =
                super.loadUser(userRequest);

        String googleId =
                oauthUser.getAttribute("sub");

        String email =
                oauthUser.getAttribute("email");

        String name =
                oauthUser.getAttribute("name");

        Boolean emailVerified =
                oauthUser.getAttribute(
                        "email_verified"
                );

        if (googleId == null
                || googleId.isBlank()) {

            throw new OAuth2AuthenticationException(
                    "Google account ID not found"
            );
        }

        if (email == null
                || email.isBlank()) {

            throw new OAuth2AuthenticationException(
                    "Google account email not found"
            );
        }

        if (!Boolean.TRUE.equals(
                emailVerified)) {

            throw new OAuth2AuthenticationException(
                    "Google email is not verified"
            );
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

            user = new User();

            user.setName(
                    name == null
                            || name.isBlank()
                            ? "Google User"
                            : name.trim()
            );

            user.setEmail(
                    normalizedEmail
            );

            user.setGoogleId(
                    googleId
            );

            user.setRole(
                    "USER"
            );

            user.setAuthProvider(
                    "GOOGLE"
            );

            user.setEmailVerified(
                    true
            );

            user.setEnabled(
                    true
            );

            user.setCreatedAt(
                    LocalDateTime.now()
            );

            user.setUpdatedAt(
                    LocalDateTime.now()
            );

            userRepository.save(
                    user
            );

            return oauthUser;
        }

        /*
         * Never reactivate a disabled account
         * through Google OAuth.
         */
        if (!user.isEnabled()) {

            throw new OAuth2AuthenticationException(
                    "Account is disabled"
            );
        }

        /*
         * Prevent a different Google account
         * from replacing an already-linked
         * Google identity.
         */
        if (user.getGoogleId() != null
                && !user.getGoogleId().isBlank()
                && !user.getGoogleId()
                        .equals(googleId)) {

            throw new OAuth2AuthenticationException(
                    "Google account does not match linked account"
            );
        }

        /*
         * Same verified Google email may be
         * linked to an existing LOCAL account.
         * Keep LOCAL provider/password intact.
         */
        user.setGoogleId(
                googleId
        );

        user.setEmailVerified(
                true
        );

        if (user.getAuthProvider() == null
                || user.getAuthProvider()
                        .isBlank()) {

            user.setAuthProvider(
                    user.getPassword() != null
                            && !user.getPassword()
                                    .isBlank()
                            ? "LOCAL"
                            : "GOOGLE"
            );
        }

        user.setUpdatedAt(
                LocalDateTime.now()
        );

        userRepository.save(
                user
        );

        return oauthUser;
    }
}