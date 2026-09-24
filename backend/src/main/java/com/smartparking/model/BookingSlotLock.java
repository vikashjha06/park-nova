package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "booking_slot_locks")
public class BookingSlotLock {

    /*
     * parkingSlotId itself is the MongoDB _id.
     *
     * This guarantees only one lock document
     * can exist for one parking slot.
     */
    @Id
    private String id;

    private String ownerId;

    private LocalDateTime lockedUntil;

    public BookingSlotLock() {
    }

    public BookingSlotLock(
            String id,
            String ownerId,
            LocalDateTime lockedUntil) {

        this.id = id;
        this.ownerId = ownerId;
        this.lockedUntil = lockedUntil;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(
            LocalDateTime lockedUntil) {

        this.lockedUntil = lockedUntil;
    }
}