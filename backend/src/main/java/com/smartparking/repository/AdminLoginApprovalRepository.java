package com.smartparking.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.smartparking.model.AdminLoginApproval;

public interface AdminLoginApprovalRepository
        extends MongoRepository<AdminLoginApproval, String> {

    Optional<AdminLoginApproval> findByRequestIdAndPollToken(
            String requestId,
            String pollToken
    );

    Optional<AdminLoginApproval> findByActionToken(
            String actionToken
    );
}