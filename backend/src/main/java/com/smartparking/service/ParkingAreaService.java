package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartparking.dto.ParkingAreaRequest;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.ParkingArea;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;

@Service
public class ParkingAreaService {

    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingAreaService(
            ParkingAreaRepository parkingAreaRepository,
            ParkingSlotRepository parkingSlotRepository) {

        this.parkingAreaRepository = parkingAreaRepository;
        this.parkingSlotRepository = parkingSlotRepository;
    }

    public ParkingArea createParkingArea(
            ParkingAreaRequest request) {

        ParkingArea parkingArea =
                new ParkingArea(
                        request.getName().trim(),
                        request.getAddress().trim(),
                        request.getCity().trim(),
                        request.getTotalSlots(),
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getGoogleMapUrl().trim(),
                        request.getGooglePlaceId()
                );

        return parkingAreaRepository.save(
                parkingArea
        );
    }

    public List<ParkingArea> getAllParkingAreas() {

        return parkingAreaRepository
                .findAll();
    }

    public List<ParkingArea> getActiveParkingAreas() {

        return parkingAreaRepository
                .findByActiveTrue();
    }

    public ParkingArea getParkingAreaById(
            String id) {

        return parkingAreaRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Parking area not found"
                        )
                );
    }

    public ParkingArea getParkingAreaWithLiveAvailability(
            String id) {

        ParkingArea parkingArea =
                getParkingAreaById(id);

        long availableCount =
                parkingSlotRepository
                        .countByParkingAreaIdAndStatusAndActiveTrue(
                                id,
                                ParkingSlotStatus.AVAILABLE
                        );

        parkingArea.setAvailableSlots(
                (int) availableCount
        );

        return parkingArea;
    }

    public ParkingArea updateParkingArea(
            String id,
            ParkingAreaRequest request) {

        ParkingArea parkingArea =
                getParkingAreaById(id);

        parkingArea.setName(
                request.getName().trim()
        );

        parkingArea.setAddress(
                request.getAddress().trim()
        );

        parkingArea.setCity(
                request.getCity().trim()
        );

        parkingArea.setTotalSlots(
                request.getTotalSlots()
        );

        parkingArea.setLatitude(
                request.getLatitude()
        );

        parkingArea.setLongitude(
                request.getLongitude()
        );

        parkingArea.setGoogleMapUrl(
                request.getGoogleMapUrl().trim()
        );

        parkingArea.setGooglePlaceId(
                request.getGooglePlaceId()
        );

        parkingArea.setUpdatedAt(
                LocalDateTime.now()
        );

        return parkingAreaRepository.save(
                parkingArea
        );
    }

    public ParkingArea setParkingAreaStatus(
            String id,
            boolean active) {

        ParkingArea parkingArea =
                getParkingAreaById(id);

        parkingArea.setActive(
                active
        );

        parkingArea.setUpdatedAt(
                LocalDateTime.now()
        );

        return parkingAreaRepository.save(
                parkingArea
        );
    }
}