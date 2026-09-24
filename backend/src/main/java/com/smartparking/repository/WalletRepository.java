package com.smartparking.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.Wallet;

@Repository
public interface WalletRepository
        extends MongoRepository<Wallet, String> {

    Optional<Wallet> findByUserId(
            String userId
    );
}