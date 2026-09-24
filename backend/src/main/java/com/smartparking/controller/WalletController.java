package com.smartparking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.exception.NotFoundException;
import com.smartparking.model.User;
import com.smartparking.model.Wallet;
import com.smartparking.model.WalletTransaction;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.WalletService;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    private final UserRepository userRepository;

    public WalletController(
            WalletService walletService,
            UserRepository userRepository) {

        this.walletService =
                walletService;

        this.userRepository =
                userRepository;
    }

    // GET MY WALLET
    @GetMapping
    public ResponseEntity<Wallet> getWallet(
            Authentication authentication) {

        User user =
                getAuthenticatedUser(
                        authentication
                );

        return ResponseEntity.ok(
                walletService.getWallet(
                        user.getId()
                )
        );
    }

    // GET MY WALLET TRANSACTIONS
    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransaction>>
    getTransactions(
            Authentication authentication) {

        User user =
                getAuthenticatedUser(
                        authentication
                );

        return ResponseEntity.ok(
                walletService.getTransactions(
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