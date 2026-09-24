package com.smartparking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.Payment;
import com.smartparking.model.PaymentStatus;
import com.smartparking.model.PaymentType;

@Repository
public interface PaymentRepository
        extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(
            String userId
    );

    List<Payment> findByUserIdAndStatus(
            String userId,
            PaymentStatus status
    );

    Optional<Payment> findByTransactionId(
            String transactionId
    );

    Optional<Payment> findByRefundTransactionId(
            String refundTransactionId
    );

    Optional<Payment>
    findFirstByPaymentTypeAndReferenceIdAndStatus(
            PaymentType paymentType,
            String referenceId,
            PaymentStatus status
    );

    Optional<Payment>
    findFirstByUserIdAndPaymentTypeAndReferenceIdAndStatus(
            String userId,
            PaymentType paymentType,
            String referenceId,
            PaymentStatus status
    );
}