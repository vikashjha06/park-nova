package com.smartparking.controller;

import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Unauthorized"));
        }

        return ResponseEntity.ok(buildProfileResponse(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Unauthorized"));
        }

        String name = request.get("name");
        String phone = request.get("phone");

        if (name == null || name.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Name must contain at least 2 characters"
                    ));
        }

        name = name.trim();

        if (name.length() > 80) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Name must not exceed 80 characters"
                    ));
        }

        if (phone == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Phone number is required"
                    ));
        }

        phone = phone.replaceAll("[\\s-]", "");

        if (!phone.matches("^[0-9]{10}$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Phone number must contain exactly 10 digits"
                    ));
        }

        user.setName(name);
        user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("profile", buildProfileResponse(savedUser));

        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String email = authentication.getName()
                .trim()
                .toLowerCase();

        return userRepository.findByEmail(email)
                .orElse(null);
    }

    private Map<String, Object> buildProfileResponse(User user) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole());
        response.put("authProvider", user.getAuthProvider());
        response.put("emailVerified", user.isEmailVerified());
        response.put("enabled", user.isEnabled());
        response.put("createdAt", user.getCreatedAt());
        response.put("updatedAt", user.getUpdatedAt());

        return response;
    }
}