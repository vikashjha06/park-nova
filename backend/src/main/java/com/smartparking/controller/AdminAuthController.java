package com.smartparking.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartparking.dto.LoginRequest;
import com.smartparking.service.AdminAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(
            AdminAuthService adminAuthService) {

        this.adminAuthService =
                adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {

            return ResponseEntity.ok(
                    adminAuthService.beginLogin(
                            request
                    )
            );

        } catch (RuntimeException exception) {

            Map<String, String> error =
                    new HashMap<>();

            error.put(
                    "message",
                    exception.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(error);
        }
    }

    @GetMapping("/decision")
    public ResponseEntity<?> decision(
            @RequestParam String token,
            @RequestParam String action) {

        try {

            return ResponseEntity.ok(
                    adminAuthService.decide(
                            token,
                            action
                    )
            );

        } catch (RuntimeException exception) {

            Map<String, String> error =
                    new HashMap<>();

            error.put(
                    "message",
                    exception.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }

    @PostMapping("/poll")
    public ResponseEntity<?> poll(
            @RequestBody Map<String, String> request) {

        try {

            return ResponseEntity.ok(
                    adminAuthService.poll(
                            request.get("requestId"),
                            request.get("pollToken")
                    )
            );

        } catch (RuntimeException exception) {

            Map<String, String> error =
                    new HashMap<>();

            error.put(
                    "message",
                    exception.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }
}