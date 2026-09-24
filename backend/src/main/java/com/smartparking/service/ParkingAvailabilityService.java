package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.ParkingArea;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;

@Service
public class ParkingAvailabilityService {

    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;

    public ParkingAvailabilityService(
            ParkingAreaRepository parkingAreaRepository,
            ParkingSlotRepository parkingSlotRepository,
            BookingRepository bookingRepository) {

        this.parkingAreaRepository =
                parkingAreaRepository;

        this.parkingSlotRepository =
                parkingSlotRepository;

        this.bookingRepository =
                bookingRepository;
    }

    public List<ParkingSlot> getAvailableSlots(
            String parkingAreaId,
            VehicleType vehicleType) {

        validateActiveParkingArea(
                parkingAreaId
        );

        if (vehicleType == null) {

            return parkingSlotRepository
                    .findByParkingAreaIdAndStatusAndActiveTrue(
                            parkingAreaId,
                            ParkingSlotStatus.AVAILABLE
                    );
        }

        return parkingSlotRepository
                .findByParkingAreaIdAndVehicleTypeAndStatusAndActiveTrue(
                        parkingAreaId,
                        vehicleType,
                        ParkingSlotStatus.AVAILABLE
                );
    }

    public List<ParkingSlot> getAvailableSlotsForTime(
            String parkingAreaId,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        validateActiveParkingArea(
                parkingAreaId
        );

        validateTimeRange(
                startTime,
                endTime
        );

        List<ParkingSlot> candidateSlots;

        if (vehicleType == null) {

            candidateSlots =
                    parkingSlotRepository
                            .findByParkingAreaIdAndActiveTrue(
                                    parkingAreaId
                            );
        } else {

            candidateSlots =
                    parkingSlotRepository
                            .findByParkingAreaIdAndVehicleTypeAndActiveTrue(
                                    parkingAreaId,
                                    vehicleType
                            );
        }

        return candidateSlots.stream()

                .filter(slot ->
                        slot.getStatus()
                                != ParkingSlotStatus.DISABLED
                )

                /*
                 * OCCUPIED means the slot is physically occupied now.
                 * It may still be reservable for a later time after
                 * the current occupancy ends, so overlap bookings are
                 * the scheduling authority for future reservations.
                 *
                 * For a request starting now/past, OCCUPIED is blocked.
                 */
                .filter(slot ->
                        slot.getStatus()
                                != ParkingSlotStatus.OCCUPIED
                        || startTime.isAfter(
                                LocalDateTime.now()
                        )
                )

                .filter(slot ->
                        !bookingRepository
                                .existsOverlappingBooking(
                                        slot.getId(),
                                        startTime,
                                        endTime
                                )
                )

                .toList();
    }

    private ParkingArea validateActiveParkingArea(
            String parkingAreaId) {

        ParkingArea parkingArea =
                parkingAreaRepository
                        .findById(parkingAreaId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Parking area not found"
                                )
                        );

        if (!parkingArea.isActive()) {

            throw new BadRequestException(
                    "Parking area is inactive"
            );
        }

        return parkingArea;
    }

    private void validateTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (startTime == null
                || endTime == null) {

            throw new BadRequestException(
                    "Start time and end time are required"
            );
        }

        if (!endTime.isAfter(startTime)) {

            throw new BadRequestException(
                    "End time must be after start time"
            );
        }

        if (startTime.isBefore(
                LocalDateTime.now()
                        .minusSeconds(5))) {

            throw new BadRequestException(
                    "Start time cannot be in the past"
            );
        }
    }
}