package com.smartparking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.model.AdminActivityLog;
import com.smartparking.repository.AdminActivityLogRepository;

@Service
public class AdminActivityLogService {

    private final AdminActivityLogRepository repository;

    public AdminActivityLogService(
            AdminActivityLogRepository repository) {

        this.repository = repository;
    }

    public AdminActivityLog log(
            String adminId,
            String adminEmail,
            String action,
            String entityType,
            String entityId,
            String description) {

        AdminActivityLog log =
                new AdminActivityLog(
                        adminId,
                        adminEmail,
                        action,
                        entityType,
                        entityId,
                        description
                );

        return repository.save(log);
    }

    public List<AdminActivityLog>
    getAllLogs() {

        return repository
                .findAllByOrderByCreatedAtDesc();
    }

    public List<AdminActivityLog>
    getLogsByAdmin(String adminId) {

        return repository
                .findByAdminIdOrderByCreatedAtDesc(
                        adminId
                );
    }

    public List<AdminActivityLog>
    getLogsByEntityType(
            String entityType) {

        return repository
                .findByEntityTypeOrderByCreatedAtDesc(
                        entityType
                );
    }
}