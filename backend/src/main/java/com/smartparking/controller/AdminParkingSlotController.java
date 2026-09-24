package com.smartparking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.ParkingSlotRequest;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.service.ParkingSlotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/parking-slots")
public class AdminParkingSlotController {

    private final ParkingSlotService parkingSlotService;

    public AdminParkingSlotController(
            ParkingSlotService parkingSlotService) {

        this.parkingSlotService = parkingSlotService;
    }

    // Create new parking slot
    @PostMapping
    public ResponseEntity<ParkingSlot> createSlot(
            @Valid @RequestBody ParkingSlotRequest request) {

        ParkingSlot slot =
                parkingSlotService.createSlot(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(slot);
    }

    // Get all slots for a parking area
    @GetMapping("/area/{parkingAreaId}")
    public ResponseEntity<List<ParkingSlot>>
    getSlotsByParkingArea(
            @PathVariable String parkingAreaId) {

        return ResponseEntity.ok(
                parkingSlotService
                        .getSlotsByParkingArea(parkingAreaId)
        );
    }

    // Get slot by ID
    @GetMapping("/{id}")
    public ResponseEntity<ParkingSlot>
    getSlotById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                parkingSlotService.getSlotById(id)
        );
    }

    // Update slot status
    @PatchMapping("/{id}/status")
    public ResponseEntity<ParkingSlot>
    updateSlotStatus(
            @PathVariable String id,
            @RequestParam ParkingSlotStatus status) {

        return ResponseEntity.ok(
                parkingSlotService
                        .updateSlotStatus(id, status)
        );
    }

    // Activate / deactivate slot
    @PatchMapping("/{id}/active")
    public ResponseEntity<ParkingSlot>
    updateSlotActive(
            @PathVariable String id,
            @RequestParam boolean active) {

        return ResponseEntity.ok(
                parkingSlotService
                        .setSlotActive(id, active)
        );
    }
}