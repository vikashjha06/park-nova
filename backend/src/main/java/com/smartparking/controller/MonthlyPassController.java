package com.smartparking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.MonthlyPassAvailabilityRequest;
import com.smartparking.dto.MonthlyPassRequest;
import com.smartparking.dto.MonthlyPassQuoteResponse;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.MonthlyPass;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.MonthlyPassService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/monthly-passes")
public class MonthlyPassController {

    private final MonthlyPassService monthlyPassService;
    private final UserRepository userRepository;

    public MonthlyPassController(
            MonthlyPassService monthlyPassService,
            UserRepository userRepository) {

        this.monthlyPassService = monthlyPassService;
        this.userRepository = userRepository;
    }

    // GET AUTHORITATIVE MONTHLY PASS PRICE QUOTE
    @PostMapping("/quote")
    public ResponseEntity<MonthlyPassQuoteResponse> quoteMonthlyPass(
            Authentication authentication,
            @Valid @RequestBody MonthlyPassRequest request) {

        /*
         * Authentication is required so this remains
         * a customer-only pricing operation.
         */
        getAuthenticatedUser(authentication);

        MonthlyPassQuoteResponse quote =
                monthlyPassService.quoteMonthlyPass(
                        request
                );

        return ResponseEntity.ok(quote);
    }
    // GET SLOTS AVAILABLE FOR REQUESTED MONTHLY PASS WINDOW
    @PostMapping("/available-slots")
    public ResponseEntity<List<ParkingSlot>> getAvailableSlots(
            Authentication authentication,
            @Valid @RequestBody MonthlyPassAvailabilityRequest request) {

        getAuthenticatedUser(authentication);

        List<ParkingSlot> slots =
                monthlyPassService
                        .getAvailableSlotsForMonthlyPass(
                                request
                        );

        return ResponseEntity.ok(slots);
    }
    // CREATE MONTHLY PASS
    @PostMapping
    public ResponseEntity<MonthlyPass> createMonthlyPass(
            Authentication authentication,
            @Valid @RequestBody MonthlyPassRequest request) {

        User user =
                getAuthenticatedUser(authentication);

        MonthlyPass monthlyPass =
                monthlyPassService.createMonthlyPass(
                        user.getId(),
                        request
                );

        return ResponseEntity.ok(
                monthlyPass
        );
    }

    // GET MY MONTHLY PASSES
    @GetMapping("/my")
    public ResponseEntity<List<MonthlyPass>> getMyPasses(
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        List<MonthlyPass> monthlyPasses =
                monthlyPassService.getUserPasses(
                        user.getId()
                );

        return ResponseEntity.ok(
                monthlyPasses
        );
    }

    // CANCEL MONTHLY PASS
    @PatchMapping("/{passId}/cancel")
    public ResponseEntity<MonthlyPass> cancelMonthlyPass(
            @PathVariable String passId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        MonthlyPass monthlyPass =
                monthlyPassService.cancelMonthlyPass(
                        passId,
                        user.getId()
                );

        return ResponseEntity.ok(
                monthlyPass
        );
    }

    // GET CURRENT AUTHENTICATED USER
    private User getAuthenticatedUser(
            Authentication authentication) {

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}