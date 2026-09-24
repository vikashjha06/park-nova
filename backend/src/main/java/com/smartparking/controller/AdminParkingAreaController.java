package com.smartparking.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.ParkingAreaRequest;
import com.smartparking.model.ParkingArea;
import com.smartparking.service.ParkingAreaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/parking-areas")
public class AdminParkingAreaController {

    private final ParkingAreaService parkingAreaService;

    public AdminParkingAreaController(
            ParkingAreaService parkingAreaService) {

        this.parkingAreaService = parkingAreaService;
    }

    // Create parking area
    @PostMapping
    public ResponseEntity<ParkingArea> createParkingArea(
            @Valid @RequestBody ParkingAreaRequest request) {

        ParkingArea parkingArea =
                parkingAreaService.createParkingArea(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(parkingArea);
    }

    // Get all parking areas
    @GetMapping
    public ResponseEntity<List<ParkingArea>>
    getAllParkingAreas() {

        return ResponseEntity.ok(
                parkingAreaService.getAllParkingAreas()
        );
    }

    // Get parking area by ID
    @GetMapping("/{id}")
    public ResponseEntity<ParkingArea>
    getParkingAreaById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                parkingAreaService.getParkingAreaById(id)
        );
    }

    // Update parking area
    @PutMapping("/{id}")
    public ResponseEntity<ParkingArea>
    updateParkingArea(
            @PathVariable String id,
            @Valid @RequestBody ParkingAreaRequest request) {

        return ResponseEntity.ok(
                parkingAreaService.updateParkingArea(
                        id,
                        request
                )
        );
    }

    // Activate / deactivate parking area
    @PatchMapping("/{id}/status")
    public ResponseEntity<ParkingArea>
    updateParkingAreaStatus(
            @PathVariable String id,
            @RequestParam boolean active) {

        return ResponseEntity.ok(
                parkingAreaService.setParkingAreaStatus(
                        id,
                        active
                )
        );
    }
}
