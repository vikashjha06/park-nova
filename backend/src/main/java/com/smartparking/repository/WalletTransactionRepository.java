package com.smartparking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.WalletTransaction;

@Repository
public interface WalletTransactionRepository
        extends MongoRepository<WalletTransaction, String> {

    List<WalletTransaction>
    findByUserIdOrderByCreatedAtDesc(
            String userId
    );

    Optional<WalletTransaction>
    findByTransactionId(
            String transactionId
    );

    Optional<WalletTransaction>
    findFirstByUserIdAndReferenceTypeAndReferenceId(
            String userId,
            String referenceType,
            String referenceId
    );
}