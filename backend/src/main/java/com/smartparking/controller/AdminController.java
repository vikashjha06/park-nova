package com.smartparking.controller;


import org.springframework.web.bind.annotation.PutMapping;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.AdminSettingsRequest;
import com.smartparking.dto.AdminUserResponse;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.AdminActivityLog;
import com.smartparking.model.AdminSettings;
import com.smartparking.model.NotificationLog;
import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.model.MonthlyPass;
import com.smartparking.model.MonthlyPassStatus;
import com.smartparking.model.Payment;
import com.smartparking.model.PaymentStatus;
import com.smartparking.model.User;
import com.smartparking.model.Wallet;
import com.smartparking.model.WalletTransaction;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.MonthlyPassRepository;
import com.smartparking.repository.NotificationLogRepository;
import com.smartparking.repository.PaymentRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.WalletRepository;
import com.smartparking.repository.WalletTransactionRepository;
import com.smartparking.service.AdminActivityLogService;
import com.smartparking.service.AdminDashboardService;
import com.smartparking.service.AdminSettingsService;
import com.smartparking.service.PaymentService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminSettingsService adminSettingsService;
    private final AdminActivityLogService adminActivityLogService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final MonthlyPassRepository monthlyPassRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public AdminController(
            AdminDashboardService adminDashboardService,
            AdminSettingsService adminSettingsService,
            AdminActivityLogService adminActivityLogService,
            PaymentService paymentService,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            NotificationLogRepository notificationLogRepository,
            MonthlyPassRepository monthlyPassRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository) {

        this.adminDashboardService =
                adminDashboardService;

        this.adminSettingsService =
                adminSettingsService;

        this.adminActivityLogService =
                adminActivityLogService;

        this.paymentService =
                paymentService;

        this.userRepository =
                userRepository;

        this.bookingRepository =
                bookingRepository;

        this.paymentRepository =
                paymentRepository;

        this.notificationLogRepository =
                notificationLogRepository;

        this.monthlyPassRepository =
                monthlyPassRepository;

        this.walletRepository =
                walletRepository;

        this.walletTransactionRepository =
                walletTransactionRepository;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>>
    getDashboard() {

        return ResponseEntity.ok(
                adminDashboardService
                        .getDashboardStats()
        );
    }

    // =========================================================
    // USERS
    // =========================================================

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>>
    getAllUsers(
            @RequestParam(required = false)
            String role,

            @RequestParam(required = false)
            Boolean enabled,

            @RequestParam(required = false)
            String search) {

        List<User> users =
                userRepository.findAll();

        List<AdminUserResponse> response =
                users.stream()

                        .filter(user ->
                                role == null
                                || (
                                    user.getRole() != null
                                    && user.getRole()
                                            .equalsIgnoreCase(role)
                                )
                        )

                        .filter(user ->
                                enabled == null
                                || user.isEnabled()
                                    == enabled
                        )

                        .filter(user ->
                                matchesUserSearch(
                                        user,
                                        search
                                )
                        )

                        .sorted(
                                Comparator.comparing(
                                        User::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )

                        .map(AdminUserResponse::new)
                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse>
    getUserById(
            @PathVariable String userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        return ResponseEntity.ok(
                new AdminUserResponse(user)
        );
    }

    // =========================================================
    // USER BLOCK / UNBLOCK
    // =========================================================

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponse>
    updateUserStatus(
            @PathVariable String userId,
            @RequestParam boolean enabled,
            Authentication authentication) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        if ("ADMIN".equalsIgnoreCase(
                user.getRole())) {

            throw new IllegalArgumentException(
                    "Administrator accounts cannot be blocked from user management"
            );
        }

        user.setEnabled(enabled);
        user.setUpdatedAt(
                LocalDateTime.now()
        );

        User savedUser =
                userRepository.save(user);

        adminActivityLogService.log(
                null,
                authentication == null
                        ? null
                        : authentication.getName(),
                enabled
                        ? "USER_UNBLOCKED"
                        : "USER_BLOCKED",
                "USER",
                savedUser.getId(),
                enabled
                        ? "User account unblocked"
                        : "User account blocked"
        );

        return ResponseEntity.ok(
                new AdminUserResponse(
                        savedUser
                )
        );
    }

    // =========================================================
    // BOOKINGS
    // =========================================================

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>>
    getAllBookings(
            @RequestParam(required = false)
            BookingStatus status,

            @RequestParam(required = false)
            String userId,

            @RequestParam(required = false)
            String parkingAreaId) {

        List<Booking> bookings =
                bookingRepository.findAll();

        List<Booking> response =
                bookings.stream()

                        .filter(booking ->
                                status == null
                                || booking.getStatus()
                                    == status
                        )

                        .filter(booking ->
                                userId == null
                                || userId.equals(
                                        booking.getUserId()
                                )
                        )

                        .filter(booking ->
                                parkingAreaId == null
                                || parkingAreaId.equals(
                                        booking.getParkingAreaId()
                                )
                        )

                        .sorted(
                                Comparator.comparing(
                                        Booking::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )

                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<Booking>
    getBookingById(
            @PathVariable String bookingId) {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Booking not found"
                                )
                        );

        return ResponseEntity.ok(
                booking
        );
    }

    // =========================================================
    // PAYMENTS
    // =========================================================

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>>
    getAllPayments(
            @RequestParam(required = false)
            PaymentStatus status,

            @RequestParam(required = false)
            String userId) {

        List<Payment> payments =
                paymentRepository.findAll();

        List<Payment> response =
                payments.stream()

                        .filter(payment ->
                                status == null
                                || payment.getStatus()
                                    == status
                        )

                        .filter(payment ->
                                userId == null
                                || userId.equals(
                                        payment.getUserId()
                                )
                        )

                        .sorted(
                                Comparator.comparing(
                                        Payment::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )

                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<Payment>
    getPaymentById(
            @PathVariable String paymentId) {

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found"
                                )
                        );

        return ResponseEntity.ok(
                payment
        );
    }

    // =========================================================
    // MONTHLY PASSES
    // =========================================================

    @GetMapping("/monthly-passes")
    public ResponseEntity<List<MonthlyPass>>
    getAllMonthlyPasses(
            @RequestParam(required = false)
            MonthlyPassStatus status,

            @RequestParam(required = false)
            String userId,

            @RequestParam(required = false)
            String parkingAreaId) {

        List<MonthlyPass> passes =
                monthlyPassRepository.findAll();

        List<MonthlyPass> response =
                passes.stream()

                        .filter(pass ->
                                status == null
                                || pass.getStatus()
                                    == status
                        )

                        .filter(pass ->
                                userId == null
                                || userId.equals(
                                        pass.getUserId()
                                )
                        )

                        .filter(pass ->
                                parkingAreaId == null
                                || parkingAreaId.equals(
                                        pass.getParkingAreaId()
                                )
                        )

                        .sorted(
                                Comparator.comparing(
                                        MonthlyPass::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )

                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/monthly-passes/{passId}")
    public ResponseEntity<MonthlyPass>
    getMonthlyPassById(
            @PathVariable String passId) {

        MonthlyPass monthlyPass =
                monthlyPassRepository
                        .findById(passId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Monthly pass not found"
                                )
                        );

        return ResponseEntity.ok(
                monthlyPass
        );
    }

    // =========================================================
    // WALLETS - READ ONLY
    // =========================================================

    @GetMapping("/wallets")
    public ResponseEntity<List<Map<String, Object>>>
    getAllWallets() {

        List<Wallet> wallets =
                walletRepository.findAll();

        List<Map<String, Object>> response =
                wallets.stream()
                        .map(wallet -> {

                            Map<String, Object> item =
                                    new LinkedHashMap<>();

                            item.put(
                                    "id",
                                    wallet.getId()
                            );

                            item.put(
                                    "userId",
                                    wallet.getUserId()
                            );

                            item.put(
                                    "balance",
                                    wallet.getBalance()
                            );

                            item.put(
                                    "createdAt",
                                    wallet.getCreatedAt()
                            );

                            item.put(
                                    "updatedAt",
                                    wallet.getUpdatedAt()
                            );

                            userRepository
                                    .findById(
                                            wallet.getUserId()
                                    )
                                    .ifPresent(user -> {

                                        item.put(
                                                "userName",
                                                user.getName()
                                        );

                                        item.put(
                                                "userEmail",
                                                user.getEmail()
                                        );
                                    });

                            return item;
                        })
                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/wallets/{userId}")
    public ResponseEntity<Map<String, Object>>
    getUserWallet(
            @PathVariable String userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        Wallet wallet =
                walletRepository
                        .findByUserId(userId)
                        .orElse(null);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "userId",
                user.getId()
        );

        response.put(
                "userName",
                user.getName()
        );

        response.put(
                "userEmail",
                user.getEmail()
        );

        response.put(
                "wallet",
                wallet
        );

        response.put(
                "transactions",
                walletTransactionRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                userId
                        )
        );

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/wallets/{userId}/transactions")
    public ResponseEntity<List<WalletTransaction>>
    getUserWalletTransactions(
            @PathVariable String userId) {

        if (!userRepository.existsById(
                userId)) {

            throw new NotFoundException(
                    "User not found"
            );
        }

        return ResponseEntity.ok(
                walletTransactionRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                userId
                        )
        );
    }


    // =========================================================
    // REFUND MANAGEMENT
    // =========================================================

    // Optional:
    // ?status=REFUND_PENDING
    // ?userId=...
    @GetMapping("/refunds")
    public ResponseEntity<List<Payment>>
    getRefunds(
            @RequestParam(required = false)
            PaymentStatus status,

            @RequestParam(required = false)
            String userId) {

        List<Payment> refunds =
                paymentRepository
                        .findAll()
                        .stream()

                        .filter(payment ->
                                payment.getStatus()
                                        == PaymentStatus.REFUND_PENDING
                                ||
                                payment.getStatus()
                                        == PaymentStatus.REFUNDED
                        )

                        .filter(payment ->
                                status == null
                                || payment.getStatus()
                                        == status
                        )

                        .filter(payment ->
                                userId == null
                                || userId.equals(
                                        payment.getUserId()
                                )
                        )

                        .sorted(
                                Comparator.comparing(
                                        Payment::getRefundRequestedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )

                        .toList();

        return ResponseEntity.ok(
                refunds
        );
    }


    @GetMapping("/refunds/{paymentId}")
    public ResponseEntity<Payment>
    getRefundByPaymentId(
            @PathVariable String paymentId) {

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found"
                                )
                        );

        if (payment.getStatus()
                != PaymentStatus.REFUND_PENDING
                &&
            payment.getStatus()
                != PaymentStatus.REFUNDED) {

            throw new IllegalArgumentException(
                    "This payment does not have a refund"
            );
        }

        return ResponseEntity.ok(
                payment
        );
    }


    // Complete an already requested refund.
    // Uses the existing PaymentService financial lock,
    // wallet-credit logic and notification flow.
    @PatchMapping("/refunds/{paymentId}/complete")
    public ResponseEntity<Payment>
    completeRefundAsAdmin(
            @PathVariable String paymentId,
            Authentication authentication) {

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found"
                                )
                        );

        Payment completedRefund =
                paymentService.completeRefund(
                        paymentId,
                        payment.getUserId()
                );

        adminActivityLogService.log(
                null,
                authentication == null
                        ? null
                        : authentication.getName(),
                "REFUND_COMPLETED",
                "PAYMENT",
                completedRefund.getId(),
                "Refund completed by administrator"
        );

        return ResponseEntity.ok(
                completedRefund
        );
    }


    // =========================================================
    // ADMIN ACTIVITY LOGS
    // =========================================================

    @GetMapping("/activity-logs")
    public ResponseEntity<List<AdminActivityLog>>
    getActivityLogs(
            @RequestParam(required = false)
            String adminId,

            @RequestParam(required = false)
            String entityType) {

        if (adminId != null
                && !adminId.isBlank()) {

            return ResponseEntity.ok(
                    adminActivityLogService
                            .getLogsByAdmin(
                                    adminId
                            )
            );
        }

        if (entityType != null
                && !entityType.isBlank()) {

            return ResponseEntity.ok(
                    adminActivityLogService
                            .getLogsByEntityType(
                                    entityType
                            )
            );
        }

        return ResponseEntity.ok(
                adminActivityLogService
                        .getAllLogs()
        );
    }


    // =========================================================
    // NOTIFICATION HISTORY
    // =========================================================

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationLog>>
    getNotificationLogs(
            @RequestParam(required = false)
            String userId,

            @RequestParam(required = false)
            String status) {

        if (userId != null
                && !userId.isBlank()) {

            return ResponseEntity.ok(
                    notificationLogRepository
                            .findByUserIdOrderByCreatedAtDesc(
                                    userId
                            )
            );
        }

        if (status != null
                && !status.isBlank()) {

            return ResponseEntity.ok(
                    notificationLogRepository
                            .findByStatusOrderByCreatedAtDesc(
                                    status
                            )
            );
        }

        return ResponseEntity.ok(
                notificationLogRepository
                        .findAllByOrderByCreatedAtDesc()
        );
    }


    @GetMapping("/notifications/{notificationId}")
    public ResponseEntity<NotificationLog>
    getNotificationLog(
            @PathVariable String notificationId) {

        NotificationLog notification =
                notificationLogRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Notification not found"
                                )
                        );

        return ResponseEntity.ok(
                notification
        );
    }


    // =========================================================
    // REPORTS & ANALYTICS
    // =========================================================

    @GetMapping("/reports/summary")
    public ResponseEntity<Map<String, Object>>
    getReportsSummary() {

        Map<String, Object> dashboard =
                adminDashboardService
                        .getDashboardStats();

        Map<String, Object> report =
                new LinkedHashMap<>();

        report.put(
                "generatedAt",
                LocalDateTime.now()
        );

        report.put(
                "dashboard",
                dashboard
        );

        report.put(
                "totalWallets",
                walletRepository.count()
        );

        report.put(
                "totalWalletTransactions",
                walletTransactionRepository.count()
        );

        report.put(
                "totalMonthlyPasses",
                monthlyPassRepository.count()
        );

        long refundRequests =
                paymentRepository
                        .findAll()
                        .stream()
                        .filter(payment ->
                                payment.getStatus()
                                        == PaymentStatus.REFUND_PENDING
                                ||
                                payment.getStatus()
                                        == PaymentStatus.REFUNDED
                        )
                        .count();

        report.put(
                "totalRefundRequests",
                refundRequests
        );

        report.put(
                "totalNotificationLogs",
                notificationLogRepository.count()
        );

        return ResponseEntity.ok(
                report
        );
    }


    // =========================================================
    // ADMIN SETTINGS
    // =========================================================

    @GetMapping("/settings")
    public ResponseEntity<AdminSettings>
    getAdminSettings() {

        return ResponseEntity.ok(
                adminSettingsService
                        .getSettings()
        );
    }


    @PutMapping("/settings")
    public ResponseEntity<AdminSettings>
    updateAdminSettings(
            @RequestBody
            AdminSettingsRequest request,

            Authentication authentication) {

        AdminSettings updated =
                adminSettingsService
                        .updateSettings(
                                request
                        );

        adminActivityLogService.log(
                null,
                authentication == null
                        ? null
                        : authentication.getName(),
                "SETTINGS_UPDATED",
                "SETTINGS",
                updated.getId(),
                "Park Nova admin settings updated"
        );

        return ResponseEntity.ok(
                updated
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean matchesUserSearch(
            User user,
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

        return containsIgnoreCase(
                    user.getName(),
                    value
                )
                || containsIgnoreCase(
                    user.getEmail(),
                    value
                )
                || containsIgnoreCase(
                    user.getPhone(),
                    value
                );
    }

    private boolean containsIgnoreCase(
            String source,
            String search) {

        return source != null
                && source
                    .toLowerCase(Locale.ROOT)
                    .contains(search);
    }
}