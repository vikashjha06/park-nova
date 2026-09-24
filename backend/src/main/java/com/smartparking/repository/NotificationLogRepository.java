package com.smartparking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.NotificationLog;

@Repository
public interface NotificationLogRepository
        extends MongoRepository<NotificationLog, String> {

    List<NotificationLog>
    findAllByOrderByCreatedAtDesc();

    List<NotificationLog>
    findByUserIdOrderByCreatedAtDesc(
            String userId
    );

    List<NotificationLog>
    findByStatusOrderByCreatedAtDesc(
            String status
    );
}