package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "wallet_transactions")
@CompoundIndex(
        name = "uk_wallet_reference",
        def = "{'userId': 1, 'referenceType': 1, 'referenceId': 1}",
        unique = true
)
public class WalletTransaction {

    @Id
    private String id;

    private String userId;

    private WalletTransactionType type;

    private double amount;

    private double balanceBefore;

    private double balanceAfter;

    private String description;

    private String referenceType;

    private String referenceId;

    /*
     * Every wallet transaction also receives
     * its own globally unique transaction ID.
     */
    @Indexed(unique = true)
    private String transactionId;

    private LocalDateTime createdAt;

    public WalletTransaction() {
    }

    public WalletTransaction(
            String userId,
            WalletTransactionType type,
            double amount,
            double balanceBefore,
            double balanceAfter,
            String description,
            String referenceType,
            String referenceId,
            String transactionId) {

        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.transactionId = transactionId;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(
            String id) {

        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(
            String userId) {

        this.userId = userId;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public void setType(
            WalletTransactionType type) {

        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(
            double amount) {

        this.amount = amount;
    }

    public double getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(
            double balanceBefore) {

        this.balanceBefore = balanceBefore;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(
            double balanceAfter) {

        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description) {

        this.description = description;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(
            String referenceType) {

        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(
            String referenceId) {

        this.referenceId = referenceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(
            String transactionId) {

        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }
}