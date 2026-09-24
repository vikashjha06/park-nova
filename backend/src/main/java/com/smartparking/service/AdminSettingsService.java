package com.smartparking.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.smartparking.dto.AdminSettingsRequest;
import com.smartparking.exception.BadRequestException;
import com.smartparking.model.AdminSettings;
import com.smartparking.repository.AdminSettingsRepository;

@Service
public class AdminSettingsService {

    private static final String SETTINGS_ID =
            "PARK_NOVA_SETTINGS";

    private final AdminSettingsRepository repository;

    public AdminSettingsService(
            AdminSettingsRepository repository) {

        this.repository = repository;
    }

    public AdminSettings getSettings() {

        return repository
                .findById(SETTINGS_ID)
                .orElseGet(
                        this::createDefaultSettings
                );
    }

    public void requireNotInMaintenance() {
        if (getSettings().isMaintenanceMode()) {
            throw new BadRequestException(
                    "New requests are unavailable during maintenance"
            );
        }
    }

    public void requireBookingAvailable() {
        AdminSettings settings = getSettings();
        if (settings.isMaintenanceMode()) {
            throw new BadRequestException(
                    "New requests are unavailable during maintenance"
            );
        }
        if (!settings.isBookingEnabled()) {
            throw new BadRequestException(
                    "New bookings are currently disabled"
            );
        }
    }
    public AdminSettings updateSettings(
            AdminSettingsRequest request) {

        AdminSettings settings =
                getSettings();

        if (request.getSystemName() != null
                && !request.getSystemName()
                        .isBlank()) {

            settings.setSystemName(
                    request.getSystemName()
                            .trim()
            );
        }

        if (request.getSupportEmail() != null) {

            settings.setSupportEmail(
                    request.getSupportEmail()
                            .trim()
            );
        }

        if (request.getMaintenanceMode()
                != null) {

            settings.setMaintenanceMode(
                    request.getMaintenanceMode()
            );
        }

        if (request.getBookingEnabled()
                != null) {

            settings.setBookingEnabled(
                    request.getBookingEnabled()
            );
        }

        settings.setUpdatedAt(
                LocalDateTime.now()
        );

        return repository.save(
                settings
        );
    }

    private AdminSettings
    createDefaultSettings() {

        AdminSettings settings =
                new AdminSettings();
        settings.setId(
                SETTINGS_ID
        );
settings.setSystemName(
                "Park Nova"
        );

        settings.setSupportEmail(
                ""
        );

        settings.setMaintenanceMode(
                false
        );

        settings.setBookingEnabled(
                true
        );

        LocalDateTime now =
                LocalDateTime.now();

        settings.setCreatedAt(now);
        settings.setUpdatedAt(now);

        return repository.save(
                settings
        );
    }
}