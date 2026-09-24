package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ConflictException;
import com.smartparking.model.Wallet;
import com.smartparking.model.WalletTransaction;
import com.smartparking.model.WalletTransactionType;
import com.smartparking.repository.WalletRepository;
import com.smartparking.repository.WalletTransactionRepository;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    private final WalletTransactionRepository
            walletTransactionRepository;

    private final MongoTemplate mongoTemplate;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository
                    walletTransactionRepository,
            MongoTemplate mongoTemplate) {

        this.walletRepository =
                walletRepository;

        this.walletTransactionRepository =
                walletTransactionRepository;

        this.mongoTemplate =
                mongoTemplate;
    }

    // =========================================================
    // GET OR CREATE WALLET
    // =========================================================

    public Wallet getWallet(
            String userId) {

        Wallet wallet =
                walletRepository
                        .findByUserId(userId)
                        .orElse(null);

        if (wallet != null) {
            return wallet;
        }

        try {

            return walletRepository.save(
                    new Wallet(userId)
            );

        } catch (DuplicateKeyException exception) {

            return walletRepository
                    .findByUserId(userId)
                    .orElseThrow(() ->
                            new ConflictException(
                                    "Wallet creation conflict. Please try again."
                            )
                    );
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public List<WalletTransaction>
    getTransactions(
            String userId) {

        return walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }

    // =========================================================
    // REFUND CREDIT
    // =========================================================

    @Transactional
    public Wallet creditRefund(
            String userId,
            double amount,
            String paymentId) {

        validateAmount(
                amount,
                "Wallet credit amount must be greater than zero"
        );

        validateReference(paymentId);

        String referenceType =
                "REFUND";

        ensureReferenceNotProcessed(
                userId,
                referenceType,
                paymentId,
                "Refund has already been credited to wallet"
        );

        /*
         * Make sure wallet exists before
         * atomic update.
         */
        getWallet(userId);

        double normalizedAmount =
                roundMoney(amount);

        Query query =
                Query.query(
                        Criteria.where("userId")
                                .is(userId)
                );

        Update update =
                new Update()
                        .inc(
                                "balance",
                                normalizedAmount
                        )
                        .set(
                                "updatedAt",
                                LocalDateTime.now()
                        );

        /*
         * returnNew(false) gives balance BEFORE
         * the atomic increment.
         */
        Wallet beforeWallet =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions
                                .options()
                                .returnNew(false),
                        Wallet.class
                );

        if (beforeWallet == null) {

            throw new NotExpectedWalletException(
                    "Wallet could not be updated"
            );
        }

        double before =
                roundMoney(
                        beforeWallet.getBalance()
                );

        double after =
                roundMoney(
                        before + normalizedAmount
                );

        WalletTransaction transaction =
                new WalletTransaction(
                        userId,
                        WalletTransactionType.CREDIT,
                        normalizedAmount,
                        before,
                        after,
                        "Refund credited to Park Nova Wallet",
                        referenceType,
                        paymentId,
                        generateWalletTransactionId()
                );

        try {

            walletTransactionRepository.save(
                    transaction
            );

        } catch (DuplicateKeyException exception) {

            /*
             * @Transactional causes the atomic
             * balance increment to roll back too.
             */
            throw new ConflictException(
                    "Refund has already been credited to wallet"
            );
        }

        return walletRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new NotExpectedWalletException(
                                "Wallet not found after credit"
                        )
                );
    }

    // =========================================================
    // PAYMENT DEBIT
    // =========================================================

    @Transactional
    public Wallet debitForPayment(
            String userId,
            double amount,
            String paymentId) {

        validateAmount(
                amount,
                "Wallet debit amount must be greater than zero"
        );

        validateReference(paymentId);

        String referenceType =
                "PAYMENT";

        ensureReferenceNotProcessed(
                userId,
                referenceType,
                paymentId,
                "Wallet has already been debited for this payment"
        );

        getWallet(userId);

        double normalizedAmount =
                roundMoney(amount);

        /*
         * Atomic conditional debit.
         *
         * MongoDB changes balance only when
         * balance >= required amount.
         *
         * Two concurrent requests cannot both
         * spend the same old balance.
         */
        Query query =
                Query.query(
                        Criteria.where("userId")
                                .is(userId)
                                .and("balance")
                                .gte(normalizedAmount)
                );

        Update update =
                new Update()
                        .inc(
                                "balance",
                                -normalizedAmount
                        )
                        .set(
                                "updatedAt",
                                LocalDateTime.now()
                        );

        Wallet beforeWallet =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions
                                .options()
                                .returnNew(false),
                        Wallet.class
                );

        if (beforeWallet == null) {

            throw new BadRequestException(
                    "Insufficient wallet balance"
            );
        }

        double before =
                roundMoney(
                        beforeWallet.getBalance()
                );

        double after =
                roundMoney(
                        before - normalizedAmount
                );

        WalletTransaction transaction =
                new WalletTransaction(
                        userId,
                        WalletTransactionType.DEBIT,
                        normalizedAmount,
                        before,
                        after,
                        "Park Nova payment",
                        referenceType,
                        paymentId,
                        generateWalletTransactionId()
                );

        try {

            walletTransactionRepository.save(
                    transaction
            );

        } catch (DuplicateKeyException exception) {

            throw new ConflictException(
                    "Wallet has already been debited for this payment"
            );
        }

        return walletRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new NotExpectedWalletException(
                                "Wallet not found after debit"
                        )
                );
    }

    private void ensureReferenceNotProcessed(
            String userId,
            String referenceType,
            String referenceId,
            String message) {

        boolean exists =
                walletTransactionRepository
                        .findFirstByUserIdAndReferenceTypeAndReferenceId(
                                userId,
                                referenceType,
                                referenceId
                        )
                        .isPresent();

        if (exists) {

            throw new ConflictException(
                    message
            );
        }
    }

    private void validateReference(
            String referenceId) {

        if (referenceId == null
                || referenceId.isBlank()) {

            throw new BadRequestException(
                    "Wallet transaction reference is required"
            );
        }
    }

    private void validateAmount(
            double amount,
            String message) {

        if (!Double.isFinite(amount)
                || amount <= 0) {

            throw new BadRequestException(
                    message
            );
        }
    }

    private double roundMoney(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    private String generateWalletTransactionId() {

        return "WLT-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }

    /*
     * Internal runtime exception.
     * Generic handler will return safe 500.
     */
    private static class NotExpectedWalletException
            extends RuntimeException {

        private NotExpectedWalletException(
                String message) {

            super(message);
        }
    }
}