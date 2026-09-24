package com.smartparking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.AdminActivityLog;

@Repository
public interface AdminActivityLogRepository
        extends MongoRepository<AdminActivityLog, String> {

    List<AdminActivityLog>
    findAllByOrderByCreatedAtDesc();

    List<AdminActivityLog>
    findByAdminIdOrderByCreatedAtDesc(
            String adminId
    );

    List<AdminActivityLog>
    findByEntityTypeOrderByCreatedAtDesc(
            String entityType
    );
}