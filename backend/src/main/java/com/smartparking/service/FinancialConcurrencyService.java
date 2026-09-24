package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.smartparking.exception.ConflictException;
import com.smartparking.model.FinancialOperationLock;

@Service
public class FinancialConcurrencyService {

    private static final long LOCK_SECONDS = 30;

    private final MongoTemplate mongoTemplate;

    public FinancialConcurrencyService(
            MongoTemplate mongoTemplate) {

        this.mongoTemplate = mongoTemplate;
    }

    public String acquire(
            String operationKey) {

        String ownerId =
                UUID.randomUUID().toString();

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime lockedUntil =
                now.plusSeconds(LOCK_SECONDS);

        try {

            mongoTemplate.insert(
                    new FinancialOperationLock(
                            operationKey,
                            ownerId,
                            lockedUntil
                    )
            );

            return ownerId;

        } catch (DuplicateKeyException ignored) {
        }

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(operationKey)
                                .and("lockedUntil")
                                .lt(now)
                );

        Update update =
                new Update()
                        .set("ownerId", ownerId)
                        .set(
                                "lockedUntil",
                                lockedUntil
                        );

        FinancialOperationLock acquired =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions
                                .options()
                                .returnNew(true),
                        FinancialOperationLock.class
                );

        if (acquired == null
                || !ownerId.equals(
                        acquired.getOwnerId())) {

            throw new ConflictException(
                    "Financial operation is already being processed"
            );
        }

        return ownerId;
    }

    public void release(
            String operationKey,
            String ownerId) {

        if (operationKey == null
                || ownerId == null) {
            return;
        }

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(operationKey)
                                .and("ownerId")
                                .is(ownerId)
                );

        mongoTemplate.remove(
                query,
                FinancialOperationLock.class
        );
    }
}