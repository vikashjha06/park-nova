package com.smartparking.service;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;

@Service
public class ParkingSlotReservationService {

    private final MongoTemplate mongoTemplate;

    public ParkingSlotReservationService(
            MongoTemplate mongoTemplate) {

        this.mongoTemplate = mongoTemplate;
    }

    /*
     * START ACTUAL PARKING SESSION
     *
     * AVAILABLE -> OCCUPIED
     *
     * Atomic update prevents two active sessions
     * from occupying the same physical slot.
     */
    public ParkingSlot occupySlot(
            String slotId,
            String parkingAreaId) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("_id").is(slotId)
                        .and("parkingAreaId").is(parkingAreaId)
                        .and("active").is(true)
                        .and("status").is(
                                ParkingSlotStatus.AVAILABLE
                        )
        );

        Update update = new Update()
                .set(
                        "status",
                        ParkingSlotStatus.OCCUPIED
                )
                .set(
                        "updatedAt",
                        LocalDateTime.now()
                );

        FindAndModifyOptions options =
                FindAndModifyOptions.options()
                        .returnNew(true);

        ParkingSlot occupiedSlot =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        options,
                        ParkingSlot.class
                );

        if (occupiedSlot == null) {

            throw new ConflictException(
                    "Parking slot is currently occupied or unavailable"
            );
        }

        return occupiedSlot;
    }

    /*
     * COMPLETE PARKING SESSION
     *
     * OCCUPIED -> AVAILABLE
     */
    public ParkingSlot releaseSlot(
            String slotId) {

        Query query = new Query(
                Criteria.where("_id")
                        .is(slotId)
        );

        Update update = new Update()
                .set(
                        "status",
                        ParkingSlotStatus.AVAILABLE
                )
                .set(
                        "updatedAt",
                        LocalDateTime.now()
                );

        FindAndModifyOptions options =
                FindAndModifyOptions.options()
                        .returnNew(true);

        ParkingSlot releasedSlot =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        options,
                        ParkingSlot.class
                );

        if (releasedSlot == null) {

            throw new NotFoundException(
                    "Parking slot not found"
            );
        }

        return releasedSlot;
    }
}