package com.smartparking.repository;

import com.smartparking.model.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VerificationTokenRepository
        extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByToken(String token);

    void deleteByUserId(String userId);
}