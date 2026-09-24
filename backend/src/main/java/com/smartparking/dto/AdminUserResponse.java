package com.smartparking.dto;

import java.time.LocalDateTime;

import com.smartparking.model.User;

public class AdminUserResponse {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String authProvider;
    private boolean emailVerified;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminUserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.authProvider = user.getAuthProvider();
        this.emailVerified = user.isEmailVerified();
        this.enabled = user.isEnabled();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}