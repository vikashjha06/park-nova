package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.smartparking.dto.PaymentRequest;
import com.smartparking.dto.RefundRequest;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.model.MonthlyPass;
import com.smartparking.model.MonthlyPassStatus;
import com.smartparking.model.Payment;
import com.smartparking.model.PaymentMethod;
import com.smartparking.model.PaymentStatus;
import com.smartparking.model.PaymentType;
import com.smartparking.model.RefundDestination;
import com.smartparking.model.Wallet;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.MonthlyPassRepository;
import com.smartparking.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final MonthlyPassRepository monthlyPassRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final FinancialConcurrencyService financialConcurrencyService;
    private final MonthlyPassService monthlyPassService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            MonthlyPassRepository monthlyPassRepository,
            WalletService walletService,
            NotificationService notificationService,
            FinancialConcurrencyService financialConcurrencyService,
            MonthlyPassService monthlyPassService) {

        this.paymentRepository =
                paymentRepository;

        this.bookingRepository =
                bookingRepository;

        this.monthlyPassRepository =
                monthlyPassRepository;

        this.walletService =
                walletService;

        this.notificationService =
                notificationService;

        this.financialConcurrencyService =
                financialConcurrencyService;

        this.monthlyPassService =
                monthlyPassService;
    }

    // =========================================================
    // CREATE PAYMENT
    // =========================================================

    public Payment createPayment(
            String userId,
            PaymentRequest request) {

        if (request == null) {

            throw new BadRequestException(
                    "Payment request is required"
            );
        }

        if (request.getPaymentType() == null
                || request.getReferenceId() == null
                || request.getReferenceId().isBlank()) {

            throw new BadRequestException(
                    "Payment type and reference are required"
            );
        }

        String lockKey =
                "CREATE:"
                        + request.getPaymentType()
                        + ":"
                        + request.getReferenceId();

        String ownerId =
                financialConcurrencyService
                        .acquire(lockKey);

        try {

            return createPaymentInternal(
                    userId,
                    request
            );

        } finally {

            financialConcurrencyService
                    .release(
                            lockKey,
                            ownerId
                    );
        }
    }

    private Payment createPaymentInternal(
            String userId,
            PaymentRequest request) {

        double amount;

        if (request.getPaymentType()
                == PaymentType.BOOKING) {

            Booking booking =
                    bookingRepository
                            .findById(
                                    request.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Booking not found"
                                    )
                            );

            if (!booking.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to pay for this booking"
                );
            }

            ensureBookingPaymentNotExpired(
                    booking
            );

            if (booking.getStatus()
                    == BookingStatus.CANCELLED) {

                throw new BadRequestException(
                        "Cancelled booking cannot be paid"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.COMPLETED) {

                throw new BadRequestException(
                        "Completed booking cannot be paid"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.ACTIVE) {

                throw new BadRequestException(
                        "Active booking cannot be paid"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.CONFIRMED) {

                throw new ConflictException(
                        "Booking is already confirmed"
                );
            }

            amount =
                    booking.getTotalAmount();
        }

        else if (request.getPaymentType()
                == PaymentType.MONTHLY_PASS) {

            MonthlyPass monthlyPass =
                    monthlyPassRepository
                            .findById(
                                    request.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Monthly pass not found"
                                    )
                            );

            if (!monthlyPass.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to pay for this monthly pass"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.CANCELLED) {

                throw new BadRequestException(
                        "Cancelled monthly pass cannot be paid"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.EXPIRED) {

                throw new BadRequestException(
                        "Expired monthly pass cannot be paid"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.ACTIVE) {

                throw new ConflictException(
                        "Monthly pass is already active"
                );
            }

            amount =
                    monthlyPass.getPricePaid();
        }

        else {

            throw new BadRequestException(
                    "Invalid payment type"
            );
        }

        if (!Double.isFinite(amount)
                || amount <= 0) {

            throw new BadRequestException(
                    "Payment amount must be greater than zero"
            );
        }

        ensureNoExistingCompletedPayment(
                request
        );

        Payment pendingPayment =
                paymentRepository
                        .findFirstByUserIdAndPaymentTypeAndReferenceIdAndStatus(
                                userId,
                                request.getPaymentType(),
                                request.getReferenceId(),
                                PaymentStatus.PENDING
                        )
                        .orElse(null);

        if (pendingPayment != null) {

            throw new ConflictException(
                    "Payment is already pending for this reference"
            );
        }

        Payment payment =
                new Payment(
                        userId,
                        request.getPaymentType(),
                        request.getReferenceId(),
                        amount,
                        request.getPaymentMethod()
                );

        payment =
                paymentRepository.save(
                        payment
                );

        if (request.getPaymentMethod()
                == PaymentMethod.WALLET) {

            return processWalletPayment(
                    payment,
                    userId
            );
        }

        return payment;
    }

    // =========================================================
    // PROCESS WALLET PAYMENT
    // =========================================================

    private Payment processWalletPayment(
            Payment payment,
            String userId) {

        try {

            walletService.debitForPayment(
                    userId,
                    payment.getAmount(),
                    payment.getId()
            );

        } catch (RuntimeException exception) {

            paymentRepository.delete(
                    payment
            );

            throw exception;
        }

        try {

            return completeSuccessfulPayment(
                    payment,
                    userId,
                    "WALLET-"
            );

        } catch (RuntimeException exception) {

            /*
             * Compensating credit.
             *
             * The unique wallet reference protects
             * against duplicate rollback credits.
             */
            try {

                walletService.creditRefund(
                        userId,
                        payment.getAmount(),
                        "ROLLBACK-"
                                + payment.getId()
                );

            } catch (RuntimeException ignored) {

                /*
                 * A production payment gateway
                 * would normally use an outbox /
                 * reconciliation worker here.
                 */
            }

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            payment.setUpdatedAt(
                    LocalDateTime.now()
            );

            Payment failedPayment =
                    paymentRepository.save(
                            payment
                    );

            notificationService
                    .sendPaymentFailed(
                            failedPayment
                    );

            throw exception;
        }
    }

    // =========================================================
    // PAYMENT SUCCESS
    // =========================================================

    public Payment markPaymentSuccess(
            String paymentId,
            String userId) {

        return withPaymentLock(
                paymentId,
                () ->
                        markPaymentSuccessInternal(
                                paymentId,
                                userId
                        )
        );
    }

    private Payment markPaymentSuccessInternal(
            String paymentId,
            String userId) {

        Payment payment =
                getUserPayment(
                        paymentId,
                        userId
                );

        if (payment.getPaymentMethod()
                == PaymentMethod.WALLET) {

            if (payment.getStatus()
                    == PaymentStatus.SUCCESS) {

                throw new ConflictException(
                        "Wallet payment is already successful"
                );
            }

            throw new BadRequestException(
                    "Wallet payments are processed automatically"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.SUCCESS) {

            throw new ConflictException(
                    "Payment already successful"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.FAILED) {

            throw new BadRequestException(
                    "Failed payment cannot be completed"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REFUND_PENDING) {

            throw new BadRequestException(
                    "Payment with pending refund cannot be completed again"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new BadRequestException(
                    "Refunded payment cannot be completed"
            );
        }

        return completeSuccessfulPayment(
                payment,
                userId,
                "TXN-"
        );
    }

    // =========================================================
    // COMPLETE SUCCESSFUL PAYMENT
    // =========================================================

    private Payment completeSuccessfulPayment(
            Payment payment,
            String userId,
            String transactionPrefix) {

        LocalDateTime now =
                LocalDateTime.now();

        Booking confirmedBooking =
                null;

        MonthlyPass activatedMonthlyPass =
                null;

        if (payment.getPaymentType()
                == PaymentType.BOOKING) {

            Booking booking =
                    bookingRepository
                            .findById(
                                    payment.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Booking not found"
                                    )
                            );

            if (!booking.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to confirm this booking"
                );
            }

            ensureBookingPaymentNotExpired(
                    booking
            );

            if (booking.getStatus()
                    == BookingStatus.CANCELLED) {

                throw new BadRequestException(
                        "Cancelled booking cannot be confirmed"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.COMPLETED) {

                throw new BadRequestException(
                        "Completed booking cannot be confirmed"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.ACTIVE) {

                throw new BadRequestException(
                        "Active booking cannot be confirmed again"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.CONFIRMED) {

                throw new ConflictException(
                        "Booking is already confirmed"
                );
            }

            booking.setStatus(
                    BookingStatus.CONFIRMED
            );

            booking.setPaymentExpiresAt(
                    null
            );

            booking.setUpdatedAt(
                    now
            );

            confirmedBooking =
                    bookingRepository.save(
                            booking
                    );
        }

        else if (payment.getPaymentType()
                == PaymentType.MONTHLY_PASS) {

            MonthlyPass monthlyPass =
                    monthlyPassRepository
                            .findById(
                                    payment.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Monthly pass not found"
                                    )
                            );

            if (!monthlyPass.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to activate this monthly pass"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.CANCELLED) {

                throw new BadRequestException(
                        "Cancelled monthly pass cannot be activated"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.EXPIRED) {

                throw new BadRequestException(
                        "Expired monthly pass cannot be activated"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.ACTIVE) {

                throw new ConflictException(
                        "Monthly pass is already active"
                );
            }

            monthlyPassService
                    .validateMonthlyPassBeforeActivation(
                            monthlyPass
                    );

            if (monthlyPass.getStartDate()
                    .isAfter(now)) {

                monthlyPass.setStatus(
                        MonthlyPassStatus.SCHEDULED
                );

            } else {

                monthlyPass.setStatus(
                        MonthlyPassStatus.ACTIVE
                );
            }

            if (monthlyPass.getStartDate() == null
                    || monthlyPass.getExpiryDate() == null) {

                throw new BadRequestException(
                        "Monthly pass reservation dates are missing"
                );
            }

            monthlyPass.setUpdatedAt(
                    now
            );

            activatedMonthlyPass =
                    monthlyPassRepository.save(
                            monthlyPass
                    );
        }

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        payment.setTransactionId(
                generateTransactionId(
                        transactionPrefix
                )
        );

        payment.setUpdatedAt(
                now
        );

        Payment savedPayment =
                paymentRepository.save(
                        payment
                );

        notificationService
                .sendPaymentSuccess(
                        savedPayment
                );

        if (confirmedBooking != null) {

            notificationService
                    .sendBookingConfirmed(
                            confirmedBooking
                    );
        }

        if (activatedMonthlyPass != null) {

            notificationService
                    .sendMonthlyPassActivated(
                            activatedMonthlyPass
                    );
        }

        return savedPayment;
    }

    // =========================================================
    // PAYMENT FAILED
    // =========================================================

    public Payment markPaymentFailed(
            String paymentId,
            String userId) {

        return withPaymentLock(
                paymentId,
                () ->
                        markPaymentFailedInternal(
                                paymentId,
                                userId
                        )
        );
    }

    private Payment markPaymentFailedInternal(
            String paymentId,
            String userId) {

        Payment payment =
                getUserPayment(
                        paymentId,
                        userId
                );

        if (payment.getPaymentMethod()
                == PaymentMethod.WALLET) {

            throw new BadRequestException(
                    "Wallet payment status is processed automatically"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.SUCCESS) {

            throw new BadRequestException(
                    "Successful payment cannot be marked as failed"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.FAILED) {

            throw new ConflictException(
                    "Payment is already marked as failed"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REFUND_PENDING) {

            throw new BadRequestException(
                    "Payment with pending refund cannot be marked as failed"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new BadRequestException(
                    "Refunded payment cannot be marked as failed"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        MonthlyPass failedMonthlyPass =
                null;

        if (payment.getPaymentType()
                == PaymentType.BOOKING) {

            Booking booking =
                    bookingRepository
                            .findById(
                                    payment.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Booking not found"
                                    )
                            );

            if (!booking.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to cancel this booking"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.COMPLETED) {

                throw new BadRequestException(
                        "Completed booking cannot be cancelled"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.ACTIVE) {

                throw new BadRequestException(
                        "Active booking cannot have failed payment"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.CONFIRMED) {

                throw new BadRequestException(
                        "Confirmed booking cannot have failed payment"
                );
            }

            if (booking.getStatus()
                    != BookingStatus.CANCELLED) {

                booking.setStatus(
                        BookingStatus.CANCELLED
                );

                booking.setPaymentExpiresAt(
                        null
                );

                booking.setUpdatedAt(
                        now
                );

                bookingRepository.save(
                        booking
                );
            }
        }

        else if (payment.getPaymentType()
                == PaymentType.MONTHLY_PASS) {

            MonthlyPass monthlyPass =
                    monthlyPassRepository
                            .findById(
                                    payment.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Monthly pass not found"
                                    )
                            );

            if (!monthlyPass.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to update this monthly pass"
                );
            }

            if (monthlyPass.getStatus()
                    == MonthlyPassStatus.ACTIVE) {

                throw new BadRequestException(
                        "Active monthly pass cannot have failed payment"
                );
            }

            if (monthlyPass.getStatus()
                    != MonthlyPassStatus.EXPIRED
                    &&
                    monthlyPass.getStatus()
                            != MonthlyPassStatus.CANCELLED) {

                monthlyPass.setStatus(
                        MonthlyPassStatus.CANCELLED
                );

                monthlyPass.setUpdatedAt(
                        now
                );

                failedMonthlyPass =
                        monthlyPassRepository.save(
                                monthlyPass
                        );
            }
        }

        payment.setStatus(
                PaymentStatus.FAILED
        );

        payment.setUpdatedAt(
                now
        );

        Payment savedPayment =
                paymentRepository.save(
                        payment
                );

        notificationService
                .sendPaymentFailed(
                        savedPayment
                );

        if (failedMonthlyPass != null) {

            notificationService
                    .sendMonthlyPassPaymentFailed(
                            failedMonthlyPass
                    );
        }

        return savedPayment;
    }

    // =========================================================
    // REQUEST REFUND
    // =========================================================

    public Payment requestRefund(
            String paymentId,
            String userId,
            RefundRequest request) {

        return withPaymentLock(
                paymentId,
                () ->
                        requestRefundInternal(
                                paymentId,
                                userId,
                                request
                        )
        );
    }

    private Payment requestRefundInternal(
            String paymentId,
            String userId,
            RefundRequest request) {

        Payment payment =
                getUserPayment(
                        paymentId,
                        userId
                );

        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {

            throw new BadRequestException(
                    "Refund reason is required"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.PENDING) {

            throw new BadRequestException(
                    "Pending payment cannot be refunded"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.FAILED) {

            throw new BadRequestException(
                    "Failed payment cannot be refunded"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REFUND_PENDING) {

            throw new ConflictException(
                    "Refund is already pending"
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new ConflictException(
                    "Payment is already refunded"
            );
        }

        if (payment.getStatus()
                != PaymentStatus.SUCCESS) {

            throw new BadRequestException(
                    "Only successful payments can be refunded"
            );
        }

        if (payment.getPaymentType()
                == PaymentType.BOOKING) {

            Booking booking =
                    bookingRepository
                            .findById(
                                    payment.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Booking not found"
                                    )
                            );

            if (!booking.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to refund this booking"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.ACTIVE) {

                throw new BadRequestException(
                        "Active booking cannot be refunded"
                );
            }

            if (booking.getStatus()
                    == BookingStatus.COMPLETED) {

                throw new BadRequestException(
                        "Completed booking cannot be refunded"
                );
            }

            if (booking.getStatus()
                    != BookingStatus.CANCELLED) {

                throw new BadRequestException(
                        "Booking must be cancelled before requesting a refund"
                );
            }
        }
        if (payment.getPaymentType()
                == PaymentType.MONTHLY_PASS) {

            MonthlyPass monthlyPass =
                    monthlyPassRepository
                            .findById(
                                    payment.getReferenceId()
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Monthly pass not found"
                                    )
                            );

            if (!monthlyPass.getUserId()
                    .equals(userId)) {

                throw new BadRequestException(
                        "You are not allowed to refund this monthly pass"
                );
            }

            /*
             * Full refund is allowed only when the pass was
             * cancelled before its selected start date/time.
             *
             * ACTIVE/used passes do not receive an automatic
             * full refund.
             */
            if (monthlyPass.getStatus()
                    != MonthlyPassStatus.CANCELLED
                    || monthlyPass.getStartDate() == null
                    || !monthlyPass.getStartDate()
                            .isAfter(LocalDateTime.now())) {

                throw new BadRequestException(
                        "Only a cancelled monthly pass that has not started yet can be refunded"
                );
            }
        }

        LocalDateTime now =
                LocalDateTime.now();

        payment.setRefundAmount(
                payment.getAmount()
        );

        payment.setRefundReason(
                request.getReason().trim()
        );

        payment.setRefundDestination(
                request.getDestination()
        );

        payment.setRefundRequestedAt(
                now
        );

        payment.setStatus(
                PaymentStatus.REFUND_PENDING
        );

        payment.setUpdatedAt(
                now
        );

        Payment savedPayment =
                paymentRepository.save(
                        payment
                );

        notificationService
                .sendRefundRequested(
                        savedPayment
                );

        return savedPayment;
    }

    // =========================================================
    // COMPLETE REFUND
    // =========================================================

    public Payment completeRefund(
            String paymentId,
            String userId) {

        return withPaymentLock(
                paymentId,
                () ->
                        completeRefundInternal(
                                paymentId,
                                userId
                        )
        );
    }

    private Payment completeRefundInternal(
            String paymentId,
            String userId) {

        Payment payment =
                getUserPayment(
                        paymentId,
                        userId
                );

        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new ConflictException(
                    "Payment is already refunded"
            );
        }

        if (payment.getStatus()
                != PaymentStatus.REFUND_PENDING) {

            throw new BadRequestException(
                    "Refund has not been requested for this payment"
            );
        }

        if (payment.getRefundDestination()
                == null) {

            payment.setRefundDestination(
                    RefundDestination.ORIGINAL_METHOD
            );
        }

        Double finalWalletBalance =
                null;

        if (payment.getRefundDestination()
                == RefundDestination.WALLET) {

            Wallet updatedWallet =
                    walletService.creditRefund(
                            userId,
                            payment.getRefundAmount(),
                            payment.getId()
                    );

            finalWalletBalance =
                    updatedWallet.getBalance();
        }

        LocalDateTime now =
                LocalDateTime.now();

        payment.setStatus(
                PaymentStatus.REFUNDED
        );

        if (payment.getRefundDestination()
                == RefundDestination.WALLET) {

            payment.setRefundTransactionId(
                    generateTransactionId(
                            "WALLET-RFND-"
                    )
            );

        } else {

            payment.setRefundTransactionId(
                    generateTransactionId(
                            "RFND-"
                    )
            );
        }

        payment.setRefundedAt(
                now
        );

        payment.setUpdatedAt(
                now
        );

        Payment savedPayment =
                paymentRepository.save(
                        payment
                );

        notificationService
                .sendRefundCompleted(
                        savedPayment,
                        finalWalletBalance
                );

        return savedPayment;
    }

    // =========================================================
    // GET MY PAYMENTS
    // =========================================================

    public List<Payment> getUserPayments(
            String userId) {

        List<Payment> payments =
                paymentRepository.findByUserId(userId);

        for (Payment payment : payments) {

            if (payment.getPaymentType()
                    == PaymentType.BOOKING) {

                bookingRepository
                        .findById(payment.getReferenceId())
                        .ifPresent(booking ->
                                payment.setReferenceStatus(
                                        booking.getStatus().name()
                                )
                        );
            }

            else if (payment.getPaymentType()
                    == PaymentType.MONTHLY_PASS) {

                monthlyPassRepository
                        .findById(payment.getReferenceId())
                        .ifPresent(monthlyPass ->
                                payment.setReferenceStatus(
                                        monthlyPass.getStatus().name()
                                )
                        );
            }
        }

        return payments;
    }

    // =========================================================
    // GET SINGLE PAYMENT
    // =========================================================

    public Payment getPayment(
            String paymentId,
            String userId) {

        return getUserPayment(
                paymentId,
                userId
        );
    }

    // =========================================================
    // PREVENT EXISTING PAYMENT
    // =========================================================

    private void ensureNoExistingCompletedPayment(
            PaymentRequest request) {

        Payment successfulPayment =
                paymentRepository
                        .findFirstByPaymentTypeAndReferenceIdAndStatus(
                                request.getPaymentType(),
                                request.getReferenceId(),
                                PaymentStatus.SUCCESS
                        )
                        .orElse(null);

        if (successfulPayment != null) {

            throw new ConflictException(
                    "Payment already completed"
            );
        }

        Payment refundPending =
                paymentRepository
                        .findFirstByPaymentTypeAndReferenceIdAndStatus(
                                request.getPaymentType(),
                                request.getReferenceId(),
                                PaymentStatus.REFUND_PENDING
                        )
                        .orElse(null);

        if (refundPending != null) {

            throw new ConflictException(
                    "Refund is pending for this reference"
            );
        }

        Payment refunded =
                paymentRepository
                        .findFirstByPaymentTypeAndReferenceIdAndStatus(
                                request.getPaymentType(),
                                request.getReferenceId(),
                                PaymentStatus.REFUNDED
                        )
                        .orElse(null);

        if (refunded != null) {

            throw new ConflictException(
                    "Payment for this reference was already refunded"
            );
        }
    }

    // =========================================================
    // CHECK BOOKING PAYMENT HOLD
    // =========================================================

    private void ensureBookingPaymentNotExpired(
            Booking booking) {

        if (booking.getStatus()
                == BookingStatus.PENDING
                &&
                booking.getPaymentExpiresAt()
                        != null
                &&
                !LocalDateTime.now()
                        .isBefore(
                                booking.getPaymentExpiresAt()
                        )) {

            booking.setStatus(
                    BookingStatus.CANCELLED
            );

            booking.setPaymentExpiresAt(
                    null
            );

            booking.setUpdatedAt(
                    LocalDateTime.now()
            );

            bookingRepository.save(
                    booking
            );

            throw new BadRequestException(
                    "Booking payment time has expired"
            );
        }
    }

    // =========================================================
    // GET USER PAYMENT
    // =========================================================

    private Payment getUserPayment(
            String paymentId,
            String userId) {

        Payment payment =
                paymentRepository
                        .findById(
                                paymentId
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found"
                                )
                        );

        if (!payment.getUserId()
                .equals(userId)) {

            throw new BadRequestException(
                    "You are not allowed to access this payment"
            );
        }

        return payment;
    }

    // =========================================================
    // FINANCIAL PAYMENT LOCK
    // =========================================================

    private Payment withPaymentLock(
            String paymentId,
            Supplier<Payment> operation) {

        if (paymentId == null
                || paymentId.isBlank()) {

            throw new BadRequestException(
                    "Payment ID is required"
            );
        }

        String lockKey =
                "PAYMENT:"
                        + paymentId;

        String ownerId =
                financialConcurrencyService
                        .acquire(lockKey);

        try {

            return operation.get();

        } finally {

            financialConcurrencyService
                    .release(
                            lockKey,
                            ownerId
                    );
        }
    }

    // =========================================================
    // TRANSACTION ID
    // =========================================================

    private String generateTransactionId(
            String prefix) {

        return prefix
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }
}
