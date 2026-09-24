package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    private String userId;

    private PaymentType paymentType;

    private String referenceId;

    private double amount;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private String transactionId;

    private Double refundAmount;

    private String refundReason;

    private RefundDestination refundDestination;

    private String refundTransactionId;

    private LocalDateTime refundRequestedAt;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Transient
    private String referenceStatus;

    public Payment() {
    }

    public Payment(
            String userId,
            PaymentType paymentType,
            String referenceId,
            double amount,
            PaymentMethod paymentMethod) {

        this.userId = userId;
        this.paymentType = paymentType;
        this.referenceId = referenceId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(
            PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(
            String referenceId) {
        this.referenceId = referenceId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(
            PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(
            PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(
            String transactionId) {
        this.transactionId = transactionId;
    }

    public Double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(
            Double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(
            String refundReason) {
        this.refundReason = refundReason;
    }

    public RefundDestination getRefundDestination() {
        return refundDestination;
    }

    public void setRefundDestination(
            RefundDestination refundDestination) {
        this.refundDestination =
                refundDestination;
    }

    public String getRefundTransactionId() {
        return refundTransactionId;
    }

    public void setRefundTransactionId(
            String refundTransactionId) {
        this.refundTransactionId =
                refundTransactionId;
    }

    public LocalDateTime getRefundRequestedAt() {
        return refundRequestedAt;
    }

    public void setRefundRequestedAt(
            LocalDateTime refundRequestedAt) {
        this.refundRequestedAt =
                refundRequestedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(
            LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getReferenceStatus() {
        return referenceStatus;
    }

    public void setReferenceStatus(String referenceStatus) {
        this.referenceStatus = referenceStatus;
    }
}