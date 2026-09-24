package com.smartparking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.BookingRequest;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.Booking;
import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(
            BookingService bookingService,
            UserRepository userRepository) {

        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // CREATE BOOKING
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        Booking booking =
                bookingService.createBooking(
                        user.getId(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(booking);
    }

    // CANCEL BOOKING
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable String bookingId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        Booking cancelledBooking =
                bookingService.cancelBooking(
                        bookingId,
                        user.getId()
                );

        return ResponseEntity.ok(
                cancelledBooking
        );
    }

    // MY BOOKINGS
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                bookingService.getUserBookings(
                        user.getId()
                )
        );
    }
    // GET SINGLE BOOKING
@GetMapping("/{bookingId}")
public ResponseEntity<Booking> getBooking(
        @PathVariable String bookingId,
        Authentication authentication) {

    User user =
            getAuthenticatedUser(authentication);

    return ResponseEntity.ok(
            bookingService.getBooking(
                    bookingId,
                    user.getId()
            )
    );
}

    // START PARKING SESSION
    @PatchMapping("/{bookingId}/start")
    public ResponseEntity<Booking> startBooking(
            @PathVariable String bookingId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        Booking startedBooking =
                bookingService.startBooking(
                        bookingId,
                        user.getId()
                );

        return ResponseEntity.ok(
                startedBooking
        );
    }

    // COMPLETE PARKING SESSION
    @PatchMapping("/{bookingId}/complete")
    public ResponseEntity<Booking> completeBooking(
            @PathVariable String bookingId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        Booking completedBooking =
                bookingService.completeBooking(
                        bookingId,
                        user.getId()
                );

        return ResponseEntity.ok(
                completedBooking
        );
    }

    // COMMON AUTHENTICATED USER LOOKUP
    private User getAuthenticatedUser(
            Authentication authentication) {

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new NotFoundException(
                                "User not found"
                        )
                );
    }
}