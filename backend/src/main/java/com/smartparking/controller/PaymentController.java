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

import com.smartparking.dto.PaymentRequest;
import com.smartparking.dto.RefundRequest;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.Payment;
import com.smartparking.model.User;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(
            PaymentService paymentService,
            UserRepository userRepository) {

        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    // CREATE PAYMENT
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            Authentication authentication,
            @Valid @RequestBody PaymentRequest request) {

        User user =
                getAuthenticatedUser(authentication);

        Payment payment =
                paymentService.createPayment(
                        user.getId(),
                        request
                );

        return ResponseEntity.ok(payment);
    }

    // MARK PAYMENT SUCCESS
    @PatchMapping("/{paymentId}/success")
    public ResponseEntity<Payment> markPaymentSuccess(
            @PathVariable String paymentId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                paymentService.markPaymentSuccess(
                        paymentId,
                        user.getId()
                )
        );
    }

    // MARK PAYMENT FAILED
    @PatchMapping("/{paymentId}/failed")
    public ResponseEntity<Payment> markPaymentFailed(
            @PathVariable String paymentId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                paymentService.markPaymentFailed(
                        paymentId,
                        user.getId()
                )
        );
    }

    // REQUEST REFUND
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> requestRefund(
            @PathVariable String paymentId,
            Authentication authentication,
            @Valid @RequestBody RefundRequest request) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                paymentService.requestRefund(
                        paymentId,
                        user.getId(),
                        request
                )
        );
    }

    // COMPLETE REFUND
    @PatchMapping("/{paymentId}/refund/complete")
    public ResponseEntity<Payment> completeRefund(
            @PathVariable String paymentId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                paymentService.completeRefund(
                        paymentId,
                        user.getId()
                )
        );
    }

    // GET MY PAYMENTS
    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                paymentService.getUserPayments(
                        user.getId()
                )
        );
    }

    // GET SINGLE PAYMENT
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable String paymentId,
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                paymentService.getPayment(
                        paymentId,
                        user.getId()
                )
        );
    }

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