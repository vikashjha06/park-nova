package com.smartparking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.dto.BookingRequest;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;

@Service
public class BookingService {

    /*
     * User gets 15 minutes to complete payment.
     */
    private static final long PAYMENT_HOLD_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingAreaRepository parkingAreaRepository;
    private final PricingService pricingService;
    private final MonthlyPassService monthlyPassService;
    private final ParkingSlotReservationService parkingSlotReservationService;
    private final NotificationService notificationService;
    private final BookingConcurrencyService bookingConcurrencyService;
    private final AdminSettingsService adminSettingsService;

    public BookingService(
            BookingRepository bookingRepository,
            ParkingSlotRepository parkingSlotRepository,
            ParkingAreaRepository parkingAreaRepository,
            PricingService pricingService,
            MonthlyPassService monthlyPassService,
            ParkingSlotReservationService parkingSlotReservationService,
            NotificationService notificationService,
            BookingConcurrencyService bookingConcurrencyService,
            AdminSettingsService adminSettingsService) {

        this.bookingRepository =
                bookingRepository;

        this.parkingSlotRepository =
                parkingSlotRepository;

        this.parkingAreaRepository =
                parkingAreaRepository;

        this.pricingService =
                pricingService;

        this.monthlyPassService =
                monthlyPassService;

        this.parkingSlotReservationService =
                parkingSlotReservationService;

        this.notificationService =
                notificationService;

        this.bookingConcurrencyService =
                bookingConcurrencyService;

        this.adminSettingsService = adminSettingsService;
    }

    // CREATE FUTURE / SCHEDULED BOOKING
    public Booking createBooking(
            String userId,
            BookingRequest request) {

        adminSettingsService.requireBookingAvailable();

        /*
         * Validate parking area.
         */
        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

        /*
         * Load selected parking slot.
         */
        ParkingSlot slot =
                parkingSlotRepository
                        .findById(
                                request.getParkingSlotId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Parking slot not found"
                                )
                        );

        /*
         * Slot must belong to selected area.
         */
        if (!slot.getParkingAreaId()
                .equals(
                        request.getParkingAreaId()
                )) {

            throw new BadRequestException(
                    "Parking slot does not belong to this parking area"
            );
        }

        /*
         * Inactive slot cannot accept bookings.
         */
        if (!slot.isActive()) {

            throw new BadRequestException(
                    "Parking slot is inactive"
            );
        }

        /*
         * DISABLED slots cannot accept bookings.
         *
         * OCCUPIED is intentionally allowed here
         * because a later future booking may still
         * be valid after the current session ends.
         */
        if (slot.getStatus()
                == ParkingSlotStatus.DISABLED) {

            throw new ConflictException(
                    "Parking slot is disabled"
            );
        }

        /*
         * Selected vehicle type must match
         * the parking slot type.
         */
        if (slot.getVehicleType()
                != request.getVehicleType()) {

            throw new BadRequestException(
                    "Vehicle type does not match parking slot"
            );
        }

        LocalDateTime startTime =
                request.getStartTime();

        LocalDateTime endTime =
                request.getEndTime();

        /*
         * Protect against missing date/time.
         */
        if (startTime == null
                || endTime == null) {

            throw new BadRequestException(
                    "Start time and end time are required"
            );
        }

        /*
         * Past booking is not allowed.
         */
        if (startTime.isBefore(
                LocalDateTime.now())) {

            throw new BadRequestException(
                    "Start time cannot be in the past"
            );
        }

        /*
         * End must be after start.
         */
        if (!endTime.isAfter(startTime)) {

            throw new BadRequestException(
                    "End time must be after start time"
            );
        }

        /*
         * Normal parking bookings must be at least 30 minutes.
         */
        long bookingMinutes =
                Duration.between(
                        startTime,
                        endTime
                ).toMinutes();

        if (bookingMinutes < 30) {

            throw new BadRequestException(
                    "Minimum booking duration is 30 minutes"
            );
        }

        /*
         * ==================================================
         * DISTRIBUTED CONCURRENT BOOKING PROTECTION
         * ==================================================
         *
         * Only one request at a time can execute:
         *
         * overlap check
         *       ->
         * booking creation
         *
         * for the same parking slot.
         *
         * The lock is stored in MongoDB instead of
         * using Java synchronized, so it can also
         * protect multiple backend instances.
         */
        String lockOwnerId =
                bookingConcurrencyService
                        .acquireSlotLock(
                                request.getParkingSlotId()
                        );

        try {

            /*
             * IMPORTANT:
             *
             * Overlap check happens AFTER the
             * distributed lock has been acquired.
             *
             * Existing:
             * 10:00 -> 10:35
             *
             * 10:35 -> 11:15 = allowed
             * 10:20 -> 10:50 = rejected
             */
            boolean overlappingBooking =
                    bookingRepository
                            .existsOverlappingBooking(
                                    request.getParkingSlotId(),
                                    startTime,
                                    endTime
                            );

            if (overlappingBooking) {

                throw new ConflictException(
                        "Parking slot is already booked for the selected time"
                );
            }

            /*
             * Normalize vehicle number.
             */
            boolean monthlyPassConflict =
                    monthlyPassService
                            .hasConflictingActivePass(
                                    userId,
                                    request.getParkingSlotId(),
                                    startTime,
                                    endTime
                            );

            String normalizedVehicleNumber =
                    request.getVehicleNumber()
                            .trim()
                            .toUpperCase();

            /*
             * Check monthly pass.
             */

boolean hasActivePass =
                    monthlyPassService
                            .isBookingCoveredByActivePass(
                                    userId,
                                    request.getParkingAreaId(),
                                    request.getParkingSlotId(),
                                    request.getVehicleType(),
                                    normalizedVehicleNumber,
                                    startTime,
                                    endTime
                            );

            if (monthlyPassConflict
                    && !hasActivePass) {

                throw new ConflictException(
                        "Parking slot is reserved by a monthly pass for the selected time"
                );
            }

            double totalAmount;

            if (hasActivePass) {

                /*
                 * Active monthly pass:
                 * no booking payment required.
                 */
                totalAmount = 0.0;

            } else {

                /*
                 * Normal booking:
                 * calculate exact parking price.
                 */
                totalAmount =
                        pricingService
                                .calculateBookingPrice(
                                        request.getParkingAreaId(),
                                        request.getVehicleType(),
                                        startTime,
                                        endTime
                                );
            }

            /*
             * Create booking object.
             */
            Booking booking =
                    new Booking(
                            userId,
                            request.getParkingAreaId(),
                            request.getParkingSlotId(),
                            normalizedVehicleNumber,
                            request.getVehicleType(),
                            startTime,
                            endTime,
                            totalAmount
                    );

            if (hasActivePass) {

                /*
                 * Monthly-pass booking requires
                 * no payment.
                 */
                booking.setStatus(
                        BookingStatus.CONFIRMED
                );

                booking.setPaymentExpiresAt(
                        null
                );

            } else {

                /*
                 * Normal booking gets a 15-minute
                 * payment hold.
                 */
                booking.setStatus(
                        BookingStatus.PENDING
                );

                booking.setPaymentExpiresAt(
                        LocalDateTime.now()
                                .plusMinutes(
                                        PAYMENT_HOLD_MINUTES
                                )
                );
            }

            /*
             * Future booking does NOT change
             * physical parking-slot status.
             *
             * Save while distributed lock is
             * still owned by this request.
             */
            Booking savedBooking =
                    bookingRepository.save(
                            booking
                    );

            /*
             * Notification happens only after
             * successful database save.
             *
             * NotificationService uses safe
             * email sending.
             */
            notificationService
                    .sendBookingCreated(
                            savedBooking
                    );

            return savedBooking;

        } finally {

            /*
             * Always release short-lived MongoDB
             * critical-section lock.
             *
             * Owner ID prevents this request from
             * deleting another request's lock.
             *
             * If backend crashes before release,
             * lock expiry allows recovery.
             */
            bookingConcurrencyService
                    .releaseSlotLock(
                            request.getParkingSlotId(),
                            lockOwnerId
                    );
        }
    }

    // CANCEL BOOKING
    public Booking cancelBooking(
            String bookingId,
            String userId) {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
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
                == BookingStatus.CANCELLED) {

            throw new ConflictException(
                    "Booking is already cancelled"
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
                    "Active booking cannot be cancelled"
            );
        }

        booking.setStatus(
                BookingStatus.CANCELLED
        );

        booking.setUpdatedAt(
                LocalDateTime.now()
        );

        Booking savedBooking =
                bookingRepository.save(
                        booking
                );

        notificationService
                .sendBookingCancelled(
                        savedBooking
                );

        return savedBooking;
    }

    // USER BOOKING HISTORY
    public List<Booking> getUserBookings(
            String userId) {

        return bookingRepository
                .findByUserId(
                        userId
                );
    }

    // GET SINGLE USER BOOKING
    public Booking getBooking(
            String bookingId,
            String userId) {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Booking not found"
                                )
                        );

        if (!booking.getUserId()
                .equals(userId)) {

            /*
             * Do not reveal another user's booking.
             */
            throw new NotFoundException(
                    "Booking not found"
            );
        }

        return booking;
    }

    // START PARKING SESSION
    public Booking startBooking(
            String bookingId,
            String userId) {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Booking not found"
                                )
                        );

        if (!booking.getUserId()
                .equals(userId)) {

            throw new BadRequestException(
                    "You are not allowed to start this booking"
            );
        }

        if (booking.getStatus()
                != BookingStatus.CONFIRMED) {

            throw new BadRequestException(
                    "Only confirmed booking can be started"
            );
        }

        if (LocalDateTime.now()
                .isBefore(
                        booking.getStartTime()
                )) {

            throw new BadRequestException(
                    "Booking cannot be started before start time"
            );
        }

        /*
         * Atomic physical transition:
         *
         * AVAILABLE -> OCCUPIED
         */
        parkingSlotReservationService
                .occupySlot(
                        booking.getParkingSlotId(),
                        booking.getParkingAreaId()
                );

        try {

            booking.setStatus(
                    BookingStatus.ACTIVE
            );

            booking.setUpdatedAt(
                    LocalDateTime.now()
            );

            Booking savedBooking =
                    bookingRepository.save(
                            booking
                    );

            notificationService
                    .sendParkingStarted(
                            savedBooking
                    );

            return savedBooking;

        } catch (Exception exception) {

            /*
             * Restore physical slot if booking
             * update fails after occupancy change.
             */
            parkingSlotReservationService
                    .releaseSlot(
                            booking.getParkingSlotId()
                    );

            throw exception;
        }
    }

    // COMPLETE PARKING SESSION
    public Booking completeBooking(
            String bookingId,
            String userId) {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Booking not found"
                                )
                        );

        if (!booking.getUserId()
                .equals(userId)) {

            throw new BadRequestException(
                    "You are not allowed to complete this booking"
            );
        }

        if (booking.getStatus()
                != BookingStatus.ACTIVE) {

            throw new BadRequestException(
                    "Only active booking can be completed"
            );
        }

        /*
         * Early exit is allowed.
         */
        booking.setStatus(
                BookingStatus.COMPLETED
        );

        booking.setUpdatedAt(
                LocalDateTime.now()
        );

        Booking savedBooking =
                bookingRepository.save(
                        booking
                );

        /*
         * Physical slot becomes available again.
         */
        parkingSlotReservationService
                .releaseSlot(
                        booking.getParkingSlotId()
                );

        notificationService
                .sendParkingCompleted(
                        savedBooking
                );

        return savedBooking;
    }
}
