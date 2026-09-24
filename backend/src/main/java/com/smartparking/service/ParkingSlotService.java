package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.dto.ParkingSlotRequest;
import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;

@Service
public class ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingAreaRepository parkingAreaRepository;

    public ParkingSlotService(
            ParkingSlotRepository parkingSlotRepository,
            ParkingAreaRepository parkingAreaRepository) {

        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingAreaRepository = parkingAreaRepository;
    }

    public ParkingSlot createSlot(
            ParkingSlotRequest request) {

        if (!parkingAreaRepository.existsById(
                request.getParkingAreaId())) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

        if (parkingSlotRepository
                .existsByParkingAreaIdAndSlotNumber(
                        request.getParkingAreaId(),
                        request.getSlotNumber().trim())) {

            throw new ConflictException(
                    "Slot number already exists"
            );
        }

        ParkingSlot slot =
                new ParkingSlot(
                        request.getParkingAreaId(),
                        request.getSlotNumber().trim(),
                        request.getVehicleType(),
                        request.getFloor(),
                        request.getZone()
                );

        slot.setSensorId(
                request.getSensorId()
        );

        return parkingSlotRepository.save(
                slot
        );
    }

    public List<ParkingSlot> getSlotsByParkingArea(
            String parkingAreaId) {

        return parkingSlotRepository
                .findByParkingAreaId(
                        parkingAreaId
                );
    }

    public ParkingSlot getSlotById(
            String id) {

        return parkingSlotRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Parking slot not found"
                        )
                );
    }

    public ParkingSlot updateSlotStatus(
            String id,
            ParkingSlotStatus status) {

        ParkingSlot slot =
                getSlotById(id);

        slot.setStatus(
                status
        );

        slot.setUpdatedAt(
                LocalDateTime.now()
        );

        return parkingSlotRepository
                .save(slot);
    }

    public ParkingSlot setSlotActive(
            String id,
            boolean active) {

        ParkingSlot slot =
                getSlotById(id);

        slot.setActive(
                active
        );

        if (!active) {

            slot.setStatus(
                    ParkingSlotStatus.DISABLED
            );

        } else if (
                slot.getStatus()
                        == ParkingSlotStatus.DISABLED
        ) {

            slot.setStatus(
                    ParkingSlotStatus.AVAILABLE
            );
        }

        slot.setUpdatedAt(
                LocalDateTime.now()
        );

        return parkingSlotRepository
                .save(slot);
    }
}