package com.smartparking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.dto.PricingPlanRequest;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.PricingPlan;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.PricingPlanRepository;

@Service
public class PricingService {

    private final PricingPlanRepository pricingPlanRepository;
    private final ParkingAreaRepository parkingAreaRepository;

    private static final LocalTime MORNING_PEAK_START =
            LocalTime.of(8, 0);

    private static final LocalTime MORNING_PEAK_END =
            LocalTime.of(11, 0);

    private static final LocalTime EVENING_PEAK_START =
            LocalTime.of(17, 0);

    private static final LocalTime EVENING_PEAK_END =
            LocalTime.of(20, 0);

    public PricingService(
            PricingPlanRepository pricingPlanRepository,
            ParkingAreaRepository parkingAreaRepository) {

        this.pricingPlanRepository = pricingPlanRepository;
        this.parkingAreaRepository = parkingAreaRepository;
    }

    public PricingPlan createPricingPlan(
            PricingPlanRequest request) {

        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

        if (pricingPlanRepository
                .existsByParkingAreaIdAndVehicleType(
                        request.getParkingAreaId(),
                        request.getVehicleType())) {

            throw new ConflictException(
                    "Pricing plan already exists for this vehicle type"
            );
        }

        PricingPlan pricingPlan =
                new PricingPlan(
                        request.getParkingAreaId(),
                        request.getVehicleType(),
                        request.getHourlyRate(),
                        request.getPeakHourlyRate(),
                        request.getMonthlyPassPrice(),
                        request.getMonthlyPassDiscountPercent()
                );

        return pricingPlanRepository.save(
                pricingPlan
        );
    }

    public List<PricingPlan> getPricingPlans(
            String parkingAreaId) {

        return pricingPlanRepository
                .findByParkingAreaIdAndActiveTrue(
                        parkingAreaId
                );
    }

    public PricingPlan getPricingPlan(
            String parkingAreaId,
            VehicleType vehicleType) {

        return pricingPlanRepository
                .findByParkingAreaIdAndVehicleTypeAndActiveTrue(
                        parkingAreaId,
                        vehicleType
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Active pricing plan not found"
                        )
                );
    }

    /*
     * EXACT DURATION PRICING
     *
     * Example at Rs.50/hour:
     *
     * 30 min = 25.00
     * 35 min = 29.17
     * 40 min = 33.33
     *
     * Peak and normal portions are calculated
     * separately when a booking crosses a boundary.
     */
    public double calculateBookingPrice(
            String parkingAreaId,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (!endTime.isAfter(startTime)) {

            throw new BadRequestException(
                    "End time must be after start time"
            );
        }

        PricingPlan pricingPlan =
                getPricingPlan(
                        parkingAreaId,
                        vehicleType
                );

        BigDecimal normalRate =
                BigDecimal.valueOf(
                        pricingPlan.getHourlyRate()
                );

        BigDecimal peakRate =
                BigDecimal.valueOf(
                        pricingPlan.getPeakHourlyRate()
                );

        BigDecimal totalAmount =
                BigDecimal.ZERO;

        LocalDateTime currentTime =
                startTime;

        while (currentTime.isBefore(endTime)) {

            LocalDateTime nextBoundary =
                    getNextPricingBoundary(
                            currentTime
                    );

            LocalDateTime segmentEnd =
                    nextBoundary.isBefore(endTime)
                            ? nextBoundary
                            : endTime;

            long seconds =
                    Duration.between(
                            currentTime,
                            segmentEnd
                    ).getSeconds();

            BigDecimal hours =
                    BigDecimal.valueOf(seconds)
                            .divide(
                                    BigDecimal.valueOf(3600),
                                    10,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal rate =
                    isPeakHour(
                            currentTime.toLocalTime()
                    )
                            ? peakRate
                            : normalRate;

            totalAmount =
                    totalAmount.add(
                            hours.multiply(rate)
                    );

            currentTime =
                    segmentEnd;
        }

        return totalAmount
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }

    private LocalDateTime getNextPricingBoundary(
            LocalDateTime currentTime) {

        LocalDate date =
                currentTime.toLocalDate();

        LocalDateTime morningStart =
                LocalDateTime.of(
                        date,
                        MORNING_PEAK_START
                );

        LocalDateTime morningEnd =
                LocalDateTime.of(
                        date,
                        MORNING_PEAK_END
                );

        LocalDateTime eveningStart =
                LocalDateTime.of(
                        date,
                        EVENING_PEAK_START
                );

        LocalDateTime eveningEnd =
                LocalDateTime.of(
                        date,
                        EVENING_PEAK_END
                );

        if (currentTime.isBefore(morningStart)) {
            return morningStart;
        }

        if (currentTime.isBefore(morningEnd)) {
            return morningEnd;
        }

        if (currentTime.isBefore(eveningStart)) {
            return eveningStart;
        }

        if (currentTime.isBefore(eveningEnd)) {
            return eveningEnd;
        }

        return LocalDateTime.of(
                date.plusDays(1),
                MORNING_PEAK_START
        );
    }

    private boolean isPeakHour(
            LocalTime time) {

        boolean morningPeak =
                !time.isBefore(
                        MORNING_PEAK_START
                )
                        &&
                time.isBefore(
                        MORNING_PEAK_END
                );

        boolean eveningPeak =
                !time.isBefore(
                        EVENING_PEAK_START
                )
                        &&
                time.isBefore(
                        EVENING_PEAK_END
                );

        return morningPeak || eveningPeak;
    }

    public PricingPlan updatePricingPlan(
            String id,
            PricingPlanRequest request) {

        PricingPlan pricingPlan =
                pricingPlanRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Pricing plan not found"
                                )
                        );

        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

        pricingPlan.setParkingAreaId(
                request.getParkingAreaId()
        );

        pricingPlan.setVehicleType(
                request.getVehicleType()
        );

        pricingPlan.setHourlyRate(
                request.getHourlyRate()
        );

        pricingPlan.setPeakHourlyRate(
                request.getPeakHourlyRate()
        );

        pricingPlan.setMonthlyPassPrice(
                request.getMonthlyPassPrice()
        );

        pricingPlan.setMonthlyPassDiscountPercent(
                request.getMonthlyPassDiscountPercent()
        );

        pricingPlan.setUpdatedAt(
                LocalDateTime.now()
        );

        return pricingPlanRepository.save(
                pricingPlan
        );
    }

    public PricingPlan setPricingPlanActive(
            String id,
            boolean active) {

        PricingPlan pricingPlan =
                pricingPlanRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Pricing plan not found"
                                )
                        );

        pricingPlan.setActive(active);

        pricingPlan.setUpdatedAt(
                LocalDateTime.now()
        );

        return pricingPlanRepository.save(
                pricingPlan
        );
    }
}