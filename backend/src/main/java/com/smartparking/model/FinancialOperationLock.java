package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "financial_operation_locks")
public class FinancialOperationLock {

    @Id
    private String id;

    private String ownerId;

    private LocalDateTime lockedUntil;

    public FinancialOperationLock() {
    }

    public FinancialOperationLock(
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