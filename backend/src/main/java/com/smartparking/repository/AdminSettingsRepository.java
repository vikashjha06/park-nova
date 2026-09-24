package com.smartparking.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.AdminSettings;

@Repository
public interface AdminSettingsRepository
        extends MongoRepository<AdminSettings, String> {
}