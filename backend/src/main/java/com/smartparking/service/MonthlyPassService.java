package com.smartparking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smartparking.dto.MonthlyPassAvailabilityRequest;
import com.smartparking.dto.MonthlyPassRequest;
import com.smartparking.dto.MonthlyPassQuoteResponse;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.model.MonthlyPass;
import com.smartparking.model.MonthlyPassStatus;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.model.PricingPlan;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.MonthlyPassRepository;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;

@Service
public class MonthlyPassService {

    private final MonthlyPassRepository monthlyPassRepository;
    private final BookingRepository bookingRepository;
    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final AdminSettingsService adminSettingsService;

    public MonthlyPassService(
            MonthlyPassRepository monthlyPassRepository,
            BookingRepository bookingRepository,
            ParkingAreaRepository parkingAreaRepository,
            ParkingSlotRepository parkingSlotRepository,
            PricingService pricingService,
            NotificationService notificationService,
            AdminSettingsService adminSettingsService) {

        this.monthlyPassRepository = monthlyPassRepository;
        this.bookingRepository = bookingRepository;
        this.parkingAreaRepository = parkingAreaRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.adminSettingsService = adminSettingsService;
    }

    // =========================================================
    // MONTHLY PASS QUOTE
    // =========================================================

    public MonthlyPassQuoteResponse quoteMonthlyPass(
            MonthlyPassRequest request) {

        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

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

        if (!slot.isActive()
                || slot.getStatus()
                        == ParkingSlotStatus.DISABLED) {

            throw new BadRequestException(
                    "Selected parking slot is disabled"
            );
        }

        if (!slot.getParkingAreaId()
                .equals(request.getParkingAreaId())) {

            throw new BadRequestException(
                    "Selected slot does not belong to this parking area"
            );
        }

        if (slot.getVehicleType()
                != request.getVehicleType()) {

            throw new BadRequestException(
                    "Selected slot does not support this vehicle type"
            );
        }

        LocalTime dailyStartTime =
                request.getDailyStartTime();

        LocalTime dailyEndTime =
                request.getDailyEndTime();

        if (dailyStartTime.equals(dailyEndTime)) {

            throw new BadRequestException(
                    "Daily start and end time cannot be the same"
            );
        }

        LocalDateTime pricingStart =
                LocalDateTime.of(
                        LocalDate.now(),
                        dailyStartTime
                );

        LocalDateTime pricingEnd =
                LocalDateTime.of(
                        LocalDate.now(),
                        dailyEndTime
                );

        if (!pricingEnd.isAfter(pricingStart)) {
            pricingEnd = pricingEnd.plusDays(1);
        }

        long dailySeconds =
                Duration.between(
                        pricingStart,
                        pricingEnd
                ).getSeconds();

        if (dailySeconds <= 0
                || dailySeconds >= 86400) {

            throw new BadRequestException(
                    "Daily parking duration must be between 1 minute and less than 24 hours"
            );
        }

        BigDecimal dailyHours =
                BigDecimal.valueOf(dailySeconds)
                        .divide(
                                BigDecimal.valueOf(3600),
                                10,
                                RoundingMode.HALF_UP
                        );

        PricingPlan pricingPlan =
                pricingService.getPricingPlan(
                        request.getParkingAreaId(),
                        request.getVehicleType()
                );

        double discountPercent =
                pricingPlan.getMonthlyPassDiscountPercent();

        if (discountPercent < 0
                || discountPercent > 100) {

            throw new BadRequestException(
                    "Monthly pass discount configuration is invalid"
            );
        }

        BigDecimal hourlyRate =
                BigDecimal.valueOf(
                        pricingPlan.getHourlyRate()
                );

        BigDecimal normalMonthlyPrice =
                hourlyRate
                        .multiply(dailyHours)
                        .multiply(BigDecimal.valueOf(30))
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal discountAmount =
                normalMonthlyPrice
                        .multiply(
                                BigDecimal.valueOf(discountPercent)
                                        .divide(
                                                BigDecimal.valueOf(100),
                                                10,
                                                RoundingMode.HALF_UP
                                        )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal finalPrice =
                normalMonthlyPrice
                        .subtract(discountAmount)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new MonthlyPassQuoteResponse(
                dailyHours
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue(),
                hourlyRate.doubleValue(),
                normalMonthlyPrice.doubleValue(),
                discountPercent,
                discountAmount.doubleValue(),
                finalPrice.doubleValue()
        );
    }
    // =========================================================
    // CREATE MONTHLY PASS
    // =========================================================

    public MonthlyPass createMonthlyPass(
            String userId,
            MonthlyPassRequest request) {

        adminSettingsService.requireNotInMaintenance();

        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

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

        if (!slot.isActive()
                || slot.getStatus()
                        == ParkingSlotStatus.DISABLED) {

            throw new BadRequestException(
                    "Selected parking slot is disabled"
            );
        }

        if (!slot.getParkingAreaId()
                .equals(request.getParkingAreaId())) {

            throw new BadRequestException(
                    "Selected slot does not belong to this parking area"
            );
        }

        if (slot.getVehicleType()
                != request.getVehicleType()) {

            throw new BadRequestException(
                    "Selected slot does not support this vehicle type"
            );
        }

        LocalTime dailyStartTime =
                request.getDailyStartTime();

        LocalTime dailyEndTime =
                request.getDailyEndTime();

        if (dailyStartTime.equals(
                dailyEndTime)) {

            throw new BadRequestException(
                    "Daily start and end time cannot be the same"
            );
        }

        /*
         * Overnight windows are supported.
         *
         * Example:
         * 22:00 -> 02:00 = 4 hours.
         */
        LocalDate pricingDate =
                LocalDate.now();

        LocalDateTime pricingStart =
                LocalDateTime.of(
                        pricingDate,
                        dailyStartTime
                );

        LocalDateTime pricingEnd =
                LocalDateTime.of(
                        pricingDate,
                        dailyEndTime
                );

        if (!pricingEnd.isAfter(
                pricingStart)) {

            pricingEnd =
                    pricingEnd.plusDays(1);
        }

        long dailySeconds =
                Duration.between(
                        pricingStart,
                        pricingEnd
                ).getSeconds();

        if (dailySeconds <= 0
                || dailySeconds >= 86400) {

            throw new BadRequestException(
                    "Daily parking duration must be between 1 minute and less than 24 hours"
            );
        }

        double dailyHours =
                BigDecimal.valueOf(
                        dailySeconds
                )
                .divide(
                        BigDecimal.valueOf(3600),
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();

        PricingPlan pricingPlan =
                pricingService.getPricingPlan(
                        request.getParkingAreaId(),
                        request.getVehicleType()
                );

        double discountPercent =
                pricingPlan
                        .getMonthlyPassDiscountPercent();

        if (discountPercent < 0
                || discountPercent > 100) {

            throw new BadRequestException(
                    "Monthly pass discount configuration is invalid"
            );
        }

        /*
         * Monthly Pass pricing:
         *
         * Normal Monthly Price
         * = Hourly Rate × Daily Parking Hours × 30
         *
         * Peak-hour pricing is intentionally NOT used for
         * monthly passes.
         */
        BigDecimal hourlyRate =
                BigDecimal.valueOf(
                        pricingPlan.getHourlyRate()
                );

        BigDecimal dailyHoursDecimal =
                BigDecimal.valueOf(
                        dailySeconds
                )
                .divide(
                        BigDecimal.valueOf(3600),
                        10,
                        RoundingMode.HALF_UP
                );

        BigDecimal normalMonthlyPrice =
                hourlyRate
                        .multiply(
                                dailyHoursDecimal
                        )
                        .multiply(
                                BigDecimal.valueOf(30)
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal discountMultiplier =
                BigDecimal.ONE.subtract(
                        BigDecimal.valueOf(
                                discountPercent
                        )
                        .divide(
                                BigDecimal.valueOf(100),
                                10,
                                RoundingMode.HALF_UP
                        )
                );

        BigDecimal finalPassPrice =
                normalMonthlyPrice
                        .multiply(
                                discountMultiplier
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        if (finalPassPrice
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new BadRequestException(
                    "Calculated monthly pass price is invalid"
            );
        }

        String vehicleNumber =
                request.getVehicleNumber()
                        .trim()
                        .toUpperCase();

        MonthlyPass activePass =
                monthlyPassRepository
                        .findFirstByUserIdAndParkingAreaIdAndVehicleTypeAndVehicleNumberAndStatus(
                                userId,
                                request.getParkingAreaId(),
                                request.getVehicleType(),
                                vehicleNumber,
                                MonthlyPassStatus.ACTIVE
                        )
                        .orElse(null);

        if (activePass != null) {

            if (activePass.getExpiryDate() != null
                    &&
                    activePass.getExpiryDate()
                            .isAfter(
                                    LocalDateTime.now()
                            )) {

                throw new ConflictException(
                        "Active monthly pass already exists"
                );
            }

            activePass.setStatus(
                    MonthlyPassStatus.EXPIRED
            );

            activePass.setUpdatedAt(
                    LocalDateTime.now()
            );

            MonthlyPass expiredPass =
                    monthlyPassRepository.save(
                            activePass
                    );

            notificationService
                    .sendMonthlyPassExpired(
                            expiredPass
                    );
        }

        MonthlyPass pendingPass =
                monthlyPassRepository
                        .findFirstByUserIdAndParkingAreaIdAndVehicleTypeAndVehicleNumberAndStatus(
                                userId,
                                request.getParkingAreaId(),
                                request.getVehicleType(),
                                vehicleNumber,
                                MonthlyPassStatus.PENDING
                        )
                        .orElse(null);

        if (pendingPass != null) {

            throw new ConflictException(
                    "Monthly pass payment is already pending"
            );
        }

        LocalDate selectedStartDate =
                request.getPassStartDate();

        if (selectedStartDate.isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "Monthly pass start date cannot be in the past"
            );
        }

        LocalDateTime startDate =
                LocalDateTime.of(
                        selectedStartDate,
                        dailyStartTime
                );

        LocalDateTime expiryDate =
                startDate.plusMonths(1);

        /*
         * Protect the selected recurring slot/time before
         * creating the monthly pass.
         */
        validateMonthlyPassReservationAvailability(
                userId,
                request.getParkingSlotId(),
                dailyStartTime,
                dailyEndTime,
                startDate,
                expiryDate
        );

        MonthlyPass monthlyPass =
                new MonthlyPass(
                        userId,
                        request.getParkingAreaId(),
                        request.getParkingSlotId(),
                        request.getVehicleType(),
                        vehicleNumber,
                        dailyStartTime,
                        dailyEndTime,
                        dailyHours,
                        pricingPlan.getHourlyRate(),
                        discountPercent,
                        normalMonthlyPrice.doubleValue(),
                        finalPassPrice.doubleValue(),
                        startDate,
                        expiryDate
                );

        MonthlyPass savedPass =
                monthlyPassRepository.save(
                        monthlyPass
                );

        notificationService
                .sendMonthlyPassCreated(
                        savedPass
                );

        return savedPass;
    }

    // =========================================================
    // GET USER PASSES
    // =========================================================

    public List<MonthlyPass> getUserPasses(
            String userId) {

        refreshExpiredPasses(
                userId
        );

        return monthlyPassRepository
                .findByUserId(userId);
    }

    // =========================================================
    // CHECK ACTIVE PASS
    // =========================================================

    public boolean hasActivePass(
            String userId,
            String parkingAreaId,
            VehicleType vehicleType,
            String vehicleNumber) {

        String normalizedVehicleNumber =
                vehicleNumber
                        .trim()
                        .toUpperCase();

        MonthlyPass monthlyPass =
                monthlyPassRepository
                        .findFirstByUserIdAndParkingAreaIdAndVehicleTypeAndVehicleNumberAndStatus(
                                userId,
                                parkingAreaId,
                                vehicleType,
                                normalizedVehicleNumber,
                                MonthlyPassStatus.ACTIVE
                        )
                        .orElse(null);

        if (monthlyPass == null) {
            return false;
        }

        if (monthlyPass.getExpiryDate() == null
                ||
                !monthlyPass.getExpiryDate()
                        .isAfter(
                                LocalDateTime.now()
                        )) {

            monthlyPass.setStatus(
                    MonthlyPassStatus.EXPIRED
            );

            monthlyPass.setUpdatedAt(
                    LocalDateTime.now()
            );

            MonthlyPass expiredPass =
                    monthlyPassRepository.save(
                            monthlyPass
                    );

            notificationService
                    .sendMonthlyPassExpired(
                            expiredPass
                    );

            return false;
        }

        return true;
    }

    // =========================================================
    // CANCEL MONTHLY PASS
    // =========================================================

    public MonthlyPass cancelMonthlyPass(
            String passId,
            String userId) {

        MonthlyPass monthlyPass =
                monthlyPassRepository
                        .findById(passId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Monthly pass not found"
                                )
                        );

        if (!monthlyPass.getUserId()
                .equals(userId)) {

            throw new BadRequestException(
                    "You are not allowed to cancel this monthly pass"
            );
        }

        if (monthlyPass.getStatus()
                == MonthlyPassStatus.CANCELLED) {

            throw new ConflictException(
                    "Monthly pass is already cancelled"
            );
        }

        if (monthlyPass.getStatus()
                == MonthlyPassStatus.EXPIRED) {

            throw new BadRequestException(
                    "Expired monthly pass cannot be cancelled"
            );
        }
        if (monthlyPass.getStatus()
                == MonthlyPassStatus.ACTIVE) {

            throw new BadRequestException(
                    "Active monthly pass cannot be cancelled"
            );
        }

        monthlyPass.setStatus(
                MonthlyPassStatus.CANCELLED
        );

        monthlyPass.setUpdatedAt(
                LocalDateTime.now()
        );

        MonthlyPass savedPass =
                monthlyPassRepository.save(
                        monthlyPass
                );

        notificationService
                .sendMonthlyPassCancelled(
                        savedPass
                );

        return savedPass;
    }

    // =========================================================
    // ADMIN CANCEL ACTIVE MONTHLY PASS
    // =========================================================
    public MonthlyPass cancelActiveMonthlyPassAsAdmin(
            String passId) {

        MonthlyPass monthlyPass =
                monthlyPassRepository.findById(passId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Monthly pass not found"
                                )
                        );

        if (monthlyPass.getStatus()
                != MonthlyPassStatus.ACTIVE) {

            throw new BadRequestException(
                    "Only an ACTIVE monthly pass can be cancelled by an administrator"
            );
        }

        monthlyPass.setStatus(
                MonthlyPassStatus.CANCELLED
        );
        monthlyPass.setUpdatedAt(
                LocalDateTime.now()
        );

        MonthlyPass savedPass =
                monthlyPassRepository.save(
                        monthlyPass
                );

        notificationService
                .sendMonthlyPassCancelled(
                        savedPass
                );

        return savedPass;
    }

    // =========================================================    // REFRESH EXPIRED ACTIVE PASSES
    // =========================================================

    private void refreshExpiredPasses(
            String userId) {

        List<MonthlyPass> activePasses =
                monthlyPassRepository
                        .findByUserIdAndStatus(
                                userId,
                                MonthlyPassStatus.ACTIVE
                        );

        LocalDateTime now =
                LocalDateTime.now();

        for (MonthlyPass monthlyPass :
                activePasses) {

            if (monthlyPass.getExpiryDate() != null
                    &&
                    !monthlyPass.getExpiryDate()
                            .isAfter(now)) {

                expirePass(
                        monthlyPass,
                        now
                );
            }
        }
    }

    // =========================================================
    // AUTOMATIC EXPIRY
    // =========================================================

    @Scheduled(fixedRate = 60000)
    public void expireMonthlyPassesAutomatically() {

        List<MonthlyPass> activePasses =
                monthlyPassRepository
                        .findByStatusAndExpiryDateBefore(
                                MonthlyPassStatus.ACTIVE,
                                LocalDateTime.now()
                        );

        LocalDateTime now =
                LocalDateTime.now();

        for (MonthlyPass monthlyPass :
                activePasses) {

            expirePass(
                    monthlyPass,
                    now
            );
        }
    }

    private void expirePass(
            MonthlyPass monthlyPass,
            LocalDateTime now) {

        monthlyPass.setStatus(
                MonthlyPassStatus.EXPIRED
        );

        monthlyPass.setUpdatedAt(
                now
        );

        MonthlyPass expiredPass =
                monthlyPassRepository.save(
                        monthlyPass
                );

        notificationService
                .sendMonthlyPassExpired(
                        expiredPass
                );
    }

    // =========================================================
    // MONTHLY PASS BOOKING COVERAGE
    // =========================================================

    public boolean isBookingCoveredByActivePass(
            String userId,
            String parkingAreaId,
            String parkingSlotId,
            VehicleType vehicleType,
            String vehicleNumber,
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd) {

        if (bookingStart == null
                || bookingEnd == null
                || !bookingEnd.isAfter(bookingStart)) {

            return false;
        }

        String normalizedVehicleNumber =
                vehicleNumber.trim().toUpperCase();

        List<MonthlyPass> activePasses =
                monthlyPassRepository
                        .findByParkingSlotIdAndStatusIn(
                                parkingSlotId,
                                List.of(
                                        MonthlyPassStatus.ACTIVE,
                                        MonthlyPassStatus.SCHEDULED
                                )
                        );

        for (MonthlyPass pass : activePasses) {

            if (!pass.getUserId().equals(userId)
                    || !pass.getParkingAreaId().equals(parkingAreaId)
                    || pass.getVehicleType() != vehicleType
                    || !pass.getVehicleNumber().equals(normalizedVehicleNumber)
                    || pass.getStartDate() == null
                    || pass.getExpiryDate() == null) {

                continue;
            }

            /*
             * Requested booking itself must be inside
             * the active lifetime of the pass.
             */
            if (bookingStart.isBefore(pass.getStartDate())
                    || bookingEnd.isAfter(pass.getExpiryDate())) {

                continue;
            }

            if (isIntervalFullyCoveredByRecurringWindow(
                    bookingStart,
                    bookingEnd,
                    pass.getDailyStartTime(),
                    pass.getDailyEndTime())) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // BLOCK OTHER BOOKINGS DURING ACTIVE PASS WINDOW
    // =========================================================

    public boolean hasConflictingActivePass(
            String userId,
            String parkingSlotId,
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd) {

        List<MonthlyPass> activePasses =
                monthlyPassRepository
                        .findByParkingSlotIdAndStatusIn(
                                parkingSlotId,
                                List.of(
                                        MonthlyPassStatus.ACTIVE,
                                        MonthlyPassStatus.SCHEDULED
                                )
                        );

        for (MonthlyPass pass : activePasses) {

            if (pass.getStartDate() == null
                    || pass.getExpiryDate() == null
                    || pass.getDailyStartTime() == null
                    || pass.getDailyEndTime() == null) {

                continue;
            }

            /*
             * No calendar overlap with pass lifetime.
             */
            if (!bookingStart.isBefore(pass.getExpiryDate())
                    || !bookingEnd.isAfter(pass.getStartDate())) {

                continue;
            }

            if (recurringWindowOverlapsInterval(
                    pass,
                    bookingStart,
                    bookingEnd)) {

                /*
                 * The pass owner is not automatically exempt here.
                 * BookingService separately verifies whether the
                 * owner's requested interval is fully covered.
                 */
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // RECURRING WINDOW HELPERS
    // =========================================================

    private boolean recurringWindowOverlapsInterval(
            MonthlyPass pass,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd) {

        LocalDate firstDate =
                requestedStart.toLocalDate()
                        .minusDays(1);

        LocalDate lastDate =
                requestedEnd.toLocalDate();

        for (LocalDate date = firstDate;
             !date.isAfter(lastDate);
             date = date.plusDays(1)) {

            LocalDateTime windowStart =
                    LocalDateTime.of(
                            date,
                            pass.getDailyStartTime()
                    );

            LocalDateTime windowEnd =
                    LocalDateTime.of(
                            date,
                            pass.getDailyEndTime()
                    );

            if (!windowEnd.isAfter(windowStart)) {
                windowEnd = windowEnd.plusDays(1);
            }

            LocalDateTime effectiveStart =
                    windowStart.isBefore(pass.getStartDate())
                            ? pass.getStartDate()
                            : windowStart;

            LocalDateTime effectiveEnd =
                    windowEnd.isAfter(pass.getExpiryDate())
                            ? pass.getExpiryDate()
                            : windowEnd;

            if (effectiveStart.isBefore(effectiveEnd)
                    && effectiveStart.isBefore(requestedEnd)
                    && effectiveEnd.isAfter(requestedStart)) {

                return true;
            }
        }

        return false;
    }

    private boolean isIntervalFullyCoveredByRecurringWindow(
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            LocalTime dailyStart,
            LocalTime dailyEnd) {

        LocalDate firstDate =
                requestedStart.toLocalDate()
                        .minusDays(1);

        LocalDate lastDate =
                requestedStart.toLocalDate();

        for (LocalDate date = firstDate;
             !date.isAfter(lastDate);
             date = date.plusDays(1)) {

            LocalDateTime windowStart =
                    LocalDateTime.of(
                            date,
                            dailyStart
                    );

            LocalDateTime windowEnd =
                    LocalDateTime.of(
                            date,
                            dailyEnd
                    );

            if (!windowEnd.isAfter(windowStart)) {
                windowEnd = windowEnd.plusDays(1);
            }

            boolean startsInside =
                    !requestedStart.isBefore(windowStart);

            boolean endsInside =
                    !requestedEnd.isAfter(windowEnd);

            if (startsInside && endsInside) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // MONTHLY PASS PURCHASE CONFLICT VALIDATION
    // =========================================================

    private void validateMonthlyPassReservationAvailability(
            String userId,
            String parkingSlotId,
            LocalTime dailyStartTime,
            LocalTime dailyEndTime,
            LocalDateTime passStart,
            LocalDateTime passEnd) {

        /*
         * First protect against another ACTIVE monthly pass
         * reserving the same physical slot/time.
         */
        List<MonthlyPass> activePasses =
                monthlyPassRepository
                        .findByParkingSlotIdAndStatusIn(
                                parkingSlotId,
                                List.of(
                                        MonthlyPassStatus.ACTIVE,
                                        MonthlyPassStatus.SCHEDULED
                                )
                        );

        for (MonthlyPass existingPass : activePasses) {

            if (existingPass.getStartDate() == null
                    || existingPass.getExpiryDate() == null
                    || existingPass.getDailyStartTime() == null
                    || existingPass.getDailyEndTime() == null) {

                continue;
            }

            if (!passStart.isBefore(existingPass.getExpiryDate())
                    || !passEnd.isAfter(existingPass.getStartDate())) {

                continue;
            }

            if (recurringPassWindowsConflict(
                    dailyStartTime,
                    dailyEndTime,
                    passStart,
                    passEnd,
                    existingPass)) {

                throw new ConflictException(
                        "Selected slot already has a monthly pass reservation for this daily time"
                );
            }
        }

        /*
         * Then protect against existing normal bookings.
         *
         * PENDING, CONFIRMED and ACTIVE bookings reserve time.
         * CANCELLED and COMPLETED do not block a new pass.
         */
        List<Booking> bookings =
                bookingRepository
                        .findByParkingSlotId(
                                parkingSlotId
                        );

        for (Booking booking : bookings) {

            if (booking.getStatus()
                    == BookingStatus.CANCELLED
                    ||
                    booking.getStatus()
                    == BookingStatus.COMPLETED) {

                continue;
            }

            if (booking.getStartTime() == null
                    || booking.getEndTime() == null) {

                continue;
            }

            if (!booking.getStartTime().isBefore(passEnd)
                    || !booking.getEndTime().isAfter(passStart)) {

                continue;
            }

            if (requestedRecurringWindowOverlapsBooking(
                    dailyStartTime,
                    dailyEndTime,
                    passStart,
                    passEnd,
                    booking.getStartTime(),
                    booking.getEndTime())) {

                throw new ConflictException(
                        "Selected slot already has a booking during the requested monthly pass time"
                );
            }
        }
    }

    private boolean recurringPassWindowsConflict(
            LocalTime requestedDailyStart,
            LocalTime requestedDailyEnd,
            LocalDateTime requestedPassStart,
            LocalDateTime requestedPassEnd,
            MonthlyPass existingPass) {

        LocalDate firstDate =
                requestedPassStart.toLocalDate()
                        .minusDays(1);

        LocalDate lastDate =
                requestedPassEnd.toLocalDate();

        for (LocalDate date = firstDate;
             !date.isAfter(lastDate);
             date = date.plusDays(1)) {

            LocalDateTime requestedWindowStart =
                    LocalDateTime.of(
                            date,
                            requestedDailyStart
                    );

            LocalDateTime requestedWindowEnd =
                    LocalDateTime.of(
                            date,
                            requestedDailyEnd
                    );

            if (!requestedWindowEnd
                    .isAfter(requestedWindowStart)) {

                requestedWindowEnd =
                        requestedWindowEnd.plusDays(1);
            }

            if (!requestedWindowStart.isBefore(requestedPassEnd)
                    || !requestedWindowEnd.isAfter(requestedPassStart)) {

                continue;
            }

            LocalDateTime existingWindowStart =
                    LocalDateTime.of(
                            date,
                            existingPass.getDailyStartTime()
                    );

            LocalDateTime existingWindowEnd =
                    LocalDateTime.of(
                            date,
                            existingPass.getDailyEndTime()
                    );

            if (!existingWindowEnd
                    .isAfter(existingWindowStart)) {

                existingWindowEnd =
                        existingWindowEnd.plusDays(1);
            }

            if (requestedWindowStart
                    .isBefore(existingWindowEnd)
                    &&
                    requestedWindowEnd
                            .isAfter(existingWindowStart)) {

                return true;
            }
        }

        return false;
    }

    private boolean requestedRecurringWindowOverlapsBooking(
            LocalTime dailyStart,
            LocalTime dailyEnd,
            LocalDateTime passStart,
            LocalDateTime passEnd,
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd) {

        LocalDate firstDate =
                bookingStart.toLocalDate()
                        .minusDays(1);

        LocalDate lastDate =
                bookingEnd.toLocalDate();

        for (LocalDate date = firstDate;
             !date.isAfter(lastDate);
             date = date.plusDays(1)) {

            LocalDateTime windowStart =
                    LocalDateTime.of(
                            date,
                            dailyStart
                    );

            LocalDateTime windowEnd =
                    LocalDateTime.of(
                            date,
                            dailyEnd
                    );

            if (!windowEnd.isAfter(windowStart)) {
                windowEnd = windowEnd.plusDays(1);
            }

            LocalDateTime effectiveStart =
                    windowStart.isBefore(passStart)
                            ? passStart
                            : windowStart;

            LocalDateTime effectiveEnd =
                    windowEnd.isAfter(passEnd)
                            ? passEnd
                            : windowEnd;

            if (effectiveStart.isBefore(effectiveEnd)
                    &&
                    effectiveStart.isBefore(bookingEnd)
                    &&
                    effectiveEnd.isAfter(bookingStart)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // MONTHLY PASS AVAILABLE SLOTS
    // =========================================================

    public List<ParkingSlot> getAvailableSlotsForMonthlyPass(
            MonthlyPassAvailabilityRequest request) {

        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

        LocalDate selectedStartDate =
                request.getPassStartDate();

        if (selectedStartDate.isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "Monthly pass start date cannot be in the past"
            );
        }

        LocalTime dailyStartTime =
                request.getDailyStartTime();

        LocalTime dailyEndTime =
                request.getDailyEndTime();

        if (dailyStartTime.equals(dailyEndTime)) {
            throw new BadRequestException(
                    "Daily start and end time cannot be the same"
            );
        }

        LocalDateTime passStart =
                LocalDateTime.of(
                        selectedStartDate,
                        dailyStartTime
                );

        LocalDateTime firstWindowEnd =
                LocalDateTime.of(
                        selectedStartDate,
                        dailyEndTime
                );

        if (!firstWindowEnd.isAfter(passStart)) {
            firstWindowEnd =
                    firstWindowEnd.plusDays(1);
        }

        long dailySeconds =
                Duration.between(
                        passStart,
                        firstWindowEnd
                ).getSeconds();

        if (dailySeconds <= 0
                || dailySeconds >= 86400) {

            throw new BadRequestException(
                    "Daily parking duration must be between 1 minute and less than 24 hours"
            );
        }

        LocalDateTime passEnd =
                passStart.plusMonths(1);

        List<ParkingSlot> candidateSlots =
                parkingSlotRepository
                        .findByParkingAreaIdAndVehicleTypeAndActiveTrue(
                                request.getParkingAreaId(),
                                request.getVehicleType()
                        );

        return candidateSlots
                .stream()
                .filter(slot ->
                        slot.getStatus()
                                != ParkingSlotStatus.DISABLED
                )
                .filter(slot ->
                        isSlotAvailableForMonthlyPass(
                                slot.getId(),
                                dailyStartTime,
                                dailyEndTime,
                                passStart,
                                passEnd
                        )
                )
                .toList();
    }

    private boolean isSlotAvailableForMonthlyPass(
            String parkingSlotId,
            LocalTime dailyStartTime,
            LocalTime dailyEndTime,
            LocalDateTime passStart,
            LocalDateTime passEnd) {

        List<MonthlyPass> activePasses =
                monthlyPassRepository
                        .findByParkingSlotIdAndStatusIn(
                                parkingSlotId,
                                List.of(
                                        MonthlyPassStatus.ACTIVE,
                                        MonthlyPassStatus.SCHEDULED
                                )
                        );

        for (MonthlyPass existingPass : activePasses) {

            if (existingPass.getStartDate() == null
                    || existingPass.getExpiryDate() == null
                    || existingPass.getDailyStartTime() == null
                    || existingPass.getDailyEndTime() == null) {
                continue;
            }

            if (!passStart.isBefore(existingPass.getExpiryDate())
                    || !passEnd.isAfter(existingPass.getStartDate())) {
                continue;
            }

            if (recurringPassWindowsConflict(
                    dailyStartTime,
                    dailyEndTime,
                    passStart,
                    passEnd,
                    existingPass)) {

                return false;
            }
        }

        List<Booking> bookings =
                bookingRepository
                        .findByParkingSlotId(
                                parkingSlotId
                        );

        for (Booking booking : bookings) {

            if (booking.getStatus() == BookingStatus.CANCELLED
                    || booking.getStatus() == BookingStatus.COMPLETED) {
                continue;
            }

            if (booking.getStartTime() == null
                    || booking.getEndTime() == null) {
                continue;
            }

            if (!booking.getStartTime().isBefore(passEnd)
                    || !booking.getEndTime().isAfter(passStart)) {
                continue;
            }

            if (requestedRecurringWindowOverlapsBooking(
                    dailyStartTime,
                    dailyEndTime,
                    passStart,
                    passEnd,
                    booking.getStartTime(),
                    booking.getEndTime())) {

                return false;
            }
        }

        return true;
    }

    // =========================================================
    // PAYMENT-TIME MONTHLY PASS RESERVATION REVALIDATION
    // =========================================================

    public void validateMonthlyPassBeforeActivation(
            MonthlyPass monthlyPass) {

        if (monthlyPass == null) {
            throw new BadRequestException(
                    "Monthly pass is required"
            );
        }

        if (monthlyPass.getParkingSlotId() == null
                || monthlyPass.getDailyStartTime() == null
                || monthlyPass.getDailyEndTime() == null
                || monthlyPass.getStartDate() == null
                || monthlyPass.getExpiryDate() == null) {

            throw new BadRequestException(
                    "Monthly pass reservation details are incomplete"
            );
        }

        validateMonthlyPassReservationAvailability(
                monthlyPass.getUserId(),
                monthlyPass.getParkingSlotId(),
                monthlyPass.getDailyStartTime(),
                monthlyPass.getDailyEndTime(),
                monthlyPass.getStartDate(),
                monthlyPass.getExpiryDate()
        );
    }

    // =========================================================
    // AUTOMATIC SCHEDULED PASS ACTIVATION
    // =========================================================

    @Scheduled(fixedRate = 15000)
    public void activateScheduledMonthlyPassesAutomatically() {

        List<MonthlyPass> scheduledPasses =
                monthlyPassRepository
                        .findAll()
                        .stream()
                        .filter(pass ->
                                pass.getStatus()
                                        == MonthlyPassStatus.SCHEDULED
                        )
                        .toList();

        LocalDateTime now =
                LocalDateTime.now();

        for (MonthlyPass monthlyPass : scheduledPasses) {

            if (monthlyPass.getStartDate() != null
                    &&
                    !monthlyPass.getStartDate()
                            .isAfter(now)) {

                monthlyPass.setStatus(
                        MonthlyPassStatus.ACTIVE
                );

                monthlyPass.setUpdatedAt(now);

                MonthlyPass activatedPass =
                        monthlyPassRepository.save(
                                monthlyPass
                        );

                notificationService
                        .sendMonthlyPassActivated(
                                activatedPass
                        );
            }
        }
    }
}