package com.smartparking.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String adminBootstrapEmail;

    public AdminBootstrap(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {

        if (adminBootstrapEmail == null
                || adminBootstrapEmail.isBlank()) {
            return;
        }

        String normalizedEmail =
                adminBootstrapEmail.trim().toLowerCase();

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            System.out.println(
                    "[ADMIN BOOTSTRAP] Account not found."
            );
            return;
        }

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            System.out.println(
                    "[ADMIN BOOTSTRAP] Account is already ADMIN."
            );
            return;
        }

        user.setRole("ADMIN");
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        System.out.println(
                "[ADMIN BOOTSTRAP] Existing account promoted to ADMIN."
        );
    }
}