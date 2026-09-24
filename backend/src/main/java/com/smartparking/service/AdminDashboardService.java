package com.smartparking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.model.ParkingArea;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.model.Payment;
import com.smartparking.model.PaymentStatus;
import com.smartparking.model.User;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.PaymentRepository;
import com.smartparking.repository.UserRepository;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            ParkingAreaRepository parkingAreaRepository,
            ParkingSlotRepository parkingSlotRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository) {

        this.userRepository =
                userRepository;

        this.parkingAreaRepository =
                parkingAreaRepository;

        this.parkingSlotRepository =
                parkingSlotRepository;

        this.bookingRepository =
                bookingRepository;

        this.paymentRepository =
                paymentRepository;
    }

    public Map<String, Object>
    getDashboardStats() {

        List<User> users =
                userRepository.findAll();

        List<ParkingArea> parkingAreas =
                parkingAreaRepository.findAll();

        List<ParkingSlot> parkingSlots =
                parkingSlotRepository.findAll();

        List<Booking> bookings =
                bookingRepository.findAll();

        List<Payment> payments =
                paymentRepository.findAll();

        long enabledUsers =
                users.stream()
                        .filter(User::isEnabled)
                        .count();

        long verifiedUsers =
                users.stream()
                        .filter(User::isEmailVerified)
                        .count();

        long activeParkingAreas =
                parkingAreas.stream()
                        .filter(ParkingArea::isActive)
                        .count();

        long availableSlots =
                countSlots(
                        parkingSlots,
                        ParkingSlotStatus.AVAILABLE
                );

        long bookedSlots =
                countSlots(
                        parkingSlots,
                        ParkingSlotStatus.BOOKED
                );

        long occupiedSlots =
                countSlots(
                        parkingSlots,
                        ParkingSlotStatus.OCCUPIED
                );

        long disabledSlots =
                countSlots(
                        parkingSlots,
                        ParkingSlotStatus.DISABLED
                );

        long pendingBookings =
                countBookings(
                        bookings,
                        BookingStatus.PENDING
                );

        long confirmedBookings =
                countBookings(
                        bookings,
                        BookingStatus.CONFIRMED
                );

        long activeBookings =
                countBookings(
                        bookings,
                        BookingStatus.ACTIVE
                );

        long completedBookings =
                countBookings(
                        bookings,
                        BookingStatus.COMPLETED
                );

        long cancelledBookings =
                countBookings(
                        bookings,
                        BookingStatus.CANCELLED
                );

        long pendingPayments =
                countPayments(
                        payments,
                        PaymentStatus.PENDING
                );

        long successfulPayments =
                countPayments(
                        payments,
                        PaymentStatus.SUCCESS
                );

        long failedPayments =
                countPayments(
                        payments,
                        PaymentStatus.FAILED
                );

        long refundPendingPayments =
                countPayments(
                        payments,
                        PaymentStatus.REFUND_PENDING
                );

        long refundedPayments =
                countPayments(
                        payments,
                        PaymentStatus.REFUNDED
                );

        double grossRevenue =
                payments.stream()
                        .filter(payment ->
                                payment.getStatus()
                                        == PaymentStatus.SUCCESS
                                || payment.getStatus()
                                        == PaymentStatus.REFUND_PENDING
                                || payment.getStatus()
                                        == PaymentStatus.REFUNDED
                        )
                        .mapToDouble(
                                Payment::getAmount
                        )
                        .sum();

        double totalRefunded =
                payments.stream()
                        .filter(payment ->
                                payment.getStatus()
                                        == PaymentStatus.REFUNDED
                        )
                        .mapToDouble(payment ->
                                payment.getRefundAmount()
                                        == null
                                        ? 0.0
                                        : payment.getRefundAmount()
                        )
                        .sum();

        double netRevenue =
                grossRevenue
                        - totalRefunded;

        Map<String, Object> stats =
                new LinkedHashMap<>();

        stats.put(
                "totalUsers",
                users.size()
        );

        stats.put(
                "enabledUsers",
                enabledUsers
        );

        stats.put(
                "verifiedUsers",
                verifiedUsers
        );

        stats.put(
                "totalParkingAreas",
                parkingAreas.size()
        );

        stats.put(
                "activeParkingAreas",
                activeParkingAreas
        );

        stats.put(
                "totalParkingSlots",
                parkingSlots.size()
        );

        stats.put(
                "availableSlots",
                availableSlots
        );

        stats.put(
                "bookedSlots",
                bookedSlots
        );

        stats.put(
                "occupiedSlots",
                occupiedSlots
        );

        stats.put(
                "disabledSlots",
                disabledSlots
        );

        stats.put(
                "totalBookings",
                bookings.size()
        );

        stats.put(
                "pendingBookings",
                pendingBookings
        );

        stats.put(
                "confirmedBookings",
                confirmedBookings
        );

        stats.put(
                "activeBookings",
                activeBookings
        );

        stats.put(
                "completedBookings",
                completedBookings
        );

        stats.put(
                "cancelledBookings",
                cancelledBookings
        );

        stats.put(
                "totalPayments",
                payments.size()
        );

        stats.put(
                "pendingPayments",
                pendingPayments
        );

        stats.put(
                "successfulPayments",
                successfulPayments
        );

        stats.put(
                "failedPayments",
                failedPayments
        );

        stats.put(
                "refundPendingPayments",
                refundPendingPayments
        );

        stats.put(
                "refundedPayments",
                refundedPayments
        );

        stats.put(
                "grossRevenue",
                money(grossRevenue)
        );

        stats.put(
                "totalRefunded",
                money(totalRefunded)
        );

        stats.put(
                "netRevenue",
                money(netRevenue)
        );

        return stats;
    }

    private long countSlots(
            List<ParkingSlot> slots,
            ParkingSlotStatus status) {

        return slots.stream()
                .filter(slot ->
                        slot.getStatus()
                                == status
                )
                .count();
    }

    private long countBookings(
            List<Booking> bookings,
            BookingStatus status) {

        return bookings.stream()
                .filter(booking ->
                        booking.getStatus()
                                == status
                )
                .count();
    }

    private long countPayments(
            List<Payment> payments,
            PaymentStatus status) {

        return payments.stream()
                .filter(payment ->
                        payment.getStatus()
                                == status
                )
                .count();
    }

    private double money(
            double amount) {

        return BigDecimal
                .valueOf(amount)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}