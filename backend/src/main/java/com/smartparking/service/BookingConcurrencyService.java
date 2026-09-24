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
import com.smartparking.model.BookingSlotLock;

@Service
public class BookingConcurrencyService {

    /*
     * The lock is intentionally short-lived.
     *
     * It protects only the critical section:
     *
     * overlap check -> booking save
     *
     * It is NOT the customer's 15-minute
     * payment reservation.
     */
    private static final long LOCK_SECONDS = 10;

    private final MongoTemplate mongoTemplate;

    public BookingConcurrencyService(
            MongoTemplate mongoTemplate) {

        this.mongoTemplate = mongoTemplate;
    }

    public String acquireSlotLock(
            String parkingSlotId) {

        String ownerId =
                UUID.randomUUID().toString();

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime lockedUntil =
                now.plusSeconds(
                        LOCK_SECONDS
                );

        /*
         * First try to create the lock document.
         *
         * Since parkingSlotId is MongoDB _id,
         * simultaneous inserts cannot both win.
         */
        try {

            BookingSlotLock newLock =
                    new BookingSlotLock(
                            parkingSlotId,
                            ownerId,
                            lockedUntil
                    );

            mongoTemplate.insert(
                    newLock
            );

            return ownerId;

        } catch (DuplicateKeyException ignored) {

            /*
             * Lock document already exists.
             * Continue below and attempt to take
             * it only when the previous lock expired.
             */
        }

        Query query =
                new Query(
                        Criteria.where("_id")
                                .is(parkingSlotId)
                                .and("lockedUntil")
                                .lt(now)
                );

        Update update =
                new Update()
                        .set(
                                "ownerId",
                                ownerId
                        )
                        .set(
                                "lockedUntil",
                                lockedUntil
                        );

        BookingSlotLock acquired =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions
                                .options()
                                .returnNew(true),
                        BookingSlotLock.class
                );

        if (acquired == null
                || !ownerId.equals(
                        acquired.getOwnerId())) {

            throw new ConflictException(
                    "Parking slot booking is currently being processed. Please try again."
            );
        }

        return ownerId;
    }

    public void releaseSlotLock(
            String parkingSlotId,
            String ownerId) {

        if (parkingSlotId == null
                || ownerId == null) {

            return;
        }

        /*
         * Owner check prevents one request from
         * releasing another request's lock.
         */
        Query query =
                new Query(
                        Criteria.where("_id")
                                .is(parkingSlotId)
                                .and("ownerId")
                                .is(ownerId)
                );

        mongoTemplate.remove(
                query,
                BookingSlotLock.class
        );
    }
}