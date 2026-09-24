package com.smartparking.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.ParkingAreaResponse;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.ParkingArea;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingAreaRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.service.ParkingAvailabilityService;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingAvailabilityService parkingAvailabilityService;

    public ParkingController(
            ParkingAreaRepository parkingAreaRepository,
            ParkingSlotRepository parkingSlotRepository,
            ParkingAvailabilityService parkingAvailabilityService) {

        this.parkingAreaRepository =
                parkingAreaRepository;

        this.parkingSlotRepository =
                parkingSlotRepository;

        this.parkingAvailabilityService =
                parkingAvailabilityService;
    }

    /*
     * GET /api/parking/areas
     *
     * Optional:
     * ?city=Ludhiana
     * ?type=CAR
     * ?search=city
     *
     * Only ACTIVE parking areas are returned.
     * availableSlots is calculated live.
     */
    @GetMapping("/areas")
    public ResponseEntity<List<ParkingAreaResponse>>
    getParkingAreas(
            @RequestParam(required = false)
            String city,

            @RequestParam(required = false)
            VehicleType type,

            @RequestParam(required = false)
            String search) {

        List<ParkingArea> areas =
                parkingAreaRepository
                        .findByActiveTrue();

        List<ParkingAreaResponse> response =
                areas.stream()

                        .filter(area ->
                                city == null
                                || (
                                    area.getCity() != null
                                    && area.getCity()
                                        .equalsIgnoreCase(
                                                city.trim()
                                        )
                                )
                        )

                        .filter(area ->
                                matchesSearch(
                                        area,
                                        search
                                )
                        )

                        .map(area ->
                                new ParkingAreaResponse(
                                        area,
                                        getLiveAvailableCount(
                                                area.getId(),
                                                type
                                        )
                                )
                        )

                        /*
                         * If vehicle type was supplied,
                         * hide areas that have no matching
                         * available physical slots.
                         */
                        .filter(area ->
                                type == null
                                || area.getAvailableSlots()
                                    > 0
                        )

                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * GET /api/parking/areas/{id}
     *
     * Returns live physical availability.
     */
    @GetMapping("/areas/{id}")
    public ResponseEntity<ParkingAreaResponse>
    getParkingAreaById(
            @PathVariable String id,

            @RequestParam(required = false)
            VehicleType type) {

        ParkingArea area =
                getActiveParkingArea(id);

        long available =
                getLiveAvailableCount(
                        id,
                        type
                );

        return ResponseEntity.ok(
                new ParkingAreaResponse(
                        area,
                        available
                )
        );
    }

    /*
     * All ACTIVE slots.
     */
    @GetMapping("/areas/{parkingAreaId}/slots")
    public ResponseEntity<List<ParkingSlot>>
    getSlots(
            @PathVariable String parkingAreaId) {

        getActiveParkingArea(
                parkingAreaId
        );

        return ResponseEntity.ok(
                parkingSlotRepository
                        .findByParkingAreaIdAndActiveTrue(
                                parkingAreaId
                        )
        );
    }

    /*
     * Physical availability right now.
     *
     * Optional:
     * ?type=CAR
     */
    @GetMapping("/areas/{parkingAreaId}/available-slots")
    public ResponseEntity<List<ParkingSlot>>
    getAvailableSlots(
            @PathVariable String parkingAreaId,

            @RequestParam(required = false)
            VehicleType type) {

        return ResponseEntity.ok(
                parkingAvailabilityService
                        .getAvailableSlots(
                                parkingAreaId,
                                type
                        )
        );
    }

    /*
     * Vehicle-specific ACTIVE slots.
     *
     * Existing endpoint preserved.
     */
    @GetMapping("/areas/{parkingAreaId}/vehicle")
    public ResponseEntity<List<ParkingSlot>>
    getSlotsByVehicleType(
            @PathVariable String parkingAreaId,

            @RequestParam VehicleType type) {

        getActiveParkingArea(
                parkingAreaId
        );

        return ResponseEntity.ok(
                parkingSlotRepository
                        .findByParkingAreaIdAndVehicleTypeAndActiveTrue(
                                parkingAreaId,
                                type
                        )
        );
    }

    /*
     * Scheduling availability.
     *
     * Example:
     *
     * /availability
     * ?type=CAR
     * &startTime=2026-09-19T10:00:00
     * &endTime=2026-09-19T11:00:00
     */
    @GetMapping(
            "/areas/{parkingAreaId}/availability"
    )
    public ResponseEntity<List<ParkingSlot>>
    getAvailability(
            @PathVariable String parkingAreaId,

            @RequestParam(required = false)
            VehicleType type,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime startTime,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime endTime) {

        return ResponseEntity.ok(
                parkingAvailabilityService
                        .getAvailableSlotsForTime(
                                parkingAreaId,
                                type,
                                startTime,
                                endTime
                        )
        );
    }

    private ParkingArea getActiveParkingArea(
            String id) {

        ParkingArea area =
                parkingAreaRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Parking area not found"
                                )
                        );

        if (!area.isActive()) {

            throw new NotFoundException(
                    "Parking area not found"
            );
        }

        return area;
    }

    private long getLiveAvailableCount(
            String parkingAreaId,
            VehicleType type) {

        if (type == null) {

            return parkingSlotRepository
                    .countByParkingAreaIdAndStatusAndActiveTrue(
                            parkingAreaId,
                            ParkingSlotStatus.AVAILABLE
                    );
        }

        return parkingSlotRepository
                .countByParkingAreaIdAndVehicleTypeAndStatusAndActiveTrue(
                        parkingAreaId,
                        type,
                        ParkingSlotStatus.AVAILABLE
                );
    }

    private boolean matchesSearch(
            ParkingArea area,
            String search) {

        if (search == null
                || search.isBlank()) {

            return true;
        }

        String value =
                search.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return contains(
                    area.getName(),
                    value
                )
                || contains(
                    area.getAddress(),
                    value
                )
                || contains(
                    area.getCity(),
                    value
                );
    }

    private boolean contains(
            String source,
            String search) {

        return source != null
                && source
                    .toLowerCase(Locale.ROOT)
                    .contains(search);
    }
}