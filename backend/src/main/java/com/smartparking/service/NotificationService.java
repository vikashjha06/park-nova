package com.smartparking.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.smartparking.model.Booking;
import com.smartparking.model.MonthlyPass;
import com.smartparking.model.Payment;
import com.smartparking.model.NotificationLog;
import com.smartparking.model.RefundDestination;
import com.smartparking.model.User;
import com.smartparking.repository.NotificationLogRepository;
import com.smartparking.repository.UserRepository;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a"
            );

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(
            EmailService emailService,
            UserRepository userRepository,
            NotificationLogRepository notificationLogRepository) {

        this.emailService = emailService;
        this.userRepository =
                userRepository;

        this.notificationLogRepository =
                notificationLogRepository;
    }

    // =========================================================
    // BOOKING NOTIFICATIONS
    // =========================================================

    public void sendBookingCreated(
            Booking booking) {

        try {

            User user =
                    findUser(
                            booking.getUserId()
                    );

            String content =
                    bookingDetails(
                            booking
                    );

            /*
             * Monthly-pass booking may already be CONFIRMED
             * at creation time because no payment is needed.
             */
            if (booking.getPaymentExpiresAt() == null
                    && booking.getStatus() != null
                    && "CONFIRMED".equals(
                            booking.getStatus().name()
                    )) {

                send(
                        user,
                        "Park Nova - Booking Confirmed",
                        "Booking Confirmed",
                        "Your parking booking has been confirmed successfully.",
                        content
                );

                return;
            }

            String message =
                    "Your parking booking has been created.";

            if (booking.getPaymentExpiresAt()
                    != null) {

                message +=
                        " Please complete payment before "
                        + formatDateTime(
                                booking.getPaymentExpiresAt()
                        )
                        + ".";
            }

            send(
                    user,
                    "Park Nova - Booking Created",
                    "Booking Created",
                    message,
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "booking created",
                    e
            );
        }
    }

    public void sendBookingConfirmed(
            Booking booking) {

        try {

            User user =
                    findUser(
                            booking.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Booking Confirmed",
                    "Booking Confirmed",
                    "Your parking booking has been confirmed successfully.",
                    bookingDetails(
                            booking
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "booking confirmed",
                    e
            );
        }
    }

    public void sendBookingCancelled(
            Booking booking) {

        try {

            User user =
                    findUser(
                            booking.getUserId()
                    );

            String content =
                    infoRow(
                            "Booking ID",
                            booking.getId()
                    )
                    + infoRow(
                            "Vehicle",
                            booking.getVehicleNumber()
                    )
                    + infoRow(
                            "Start Time",
                            formatDateTime(
                                    booking.getStartTime()
                            )
                    )
                    + infoRow(
                            "Status",
                            booking.getStatus().name()
                    );

            send(
                    user,
                    "Park Nova - Booking Cancelled",
                    "Booking Cancelled",
                    "Your Park Nova booking has been cancelled.",
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "booking cancelled",
                    e
            );
        }
    }

    public void sendParkingStarted(
            Booking booking) {

        try {

            User user =
                    findUser(
                            booking.getUserId()
                    );

            String content =
                    infoRow(
                            "Booking ID",
                            booking.getId()
                    )
                    + infoRow(
                            "Vehicle",
                            booking.getVehicleNumber()
                    )
                    + infoRow(
                            "Status",
                            booking.getStatus().name()
                    );

            send(
                    user,
                    "Park Nova - Parking Started",
                    "Parking Session Started",
                    "Your parking session is now active.",
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "parking started",
                    e
            );
        }
    }

    public void sendParkingCompleted(
            Booking booking) {

        try {

            User user =
                    findUser(
                            booking.getUserId()
                    );

            String content =
                    infoRow(
                            "Booking ID",
                            booking.getId()
                    )
                    + infoRow(
                            "Vehicle",
                            booking.getVehicleNumber()
                    )
                    + infoRow(
                            "Amount",
                            formatMoney(
                                    booking.getTotalAmount()
                            )
                    )
                    + infoRow(
                            "Status",
                            booking.getStatus().name()
                    );

            send(
                    user,
                    "Park Nova - Parking Completed",
                    "Parking Completed",
                    "Your parking session has been completed successfully.",
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "parking completed",
                    e
            );
        }
    }

    // =========================================================
    // PAYMENT NOTIFICATIONS
    // =========================================================

    public void sendPaymentSuccess(
            Payment payment) {

        try {

            User user =
                    findUser(
                            payment.getUserId()
                    );

            String content =
                    paymentDetails(payment)
                    + infoRow(
                            "Transaction ID",
                            payment.getTransactionId()
                    );

            String message;

            if (payment.getPaymentMethod()
                    != null
                    &&
                    "WALLET".equals(
                            payment.getPaymentMethod()
                                    .name()
                    )) {

                message =
                        "Your payment using Park Nova Wallet "
                        + "was completed successfully.";

            } else {

                message =
                        "Your payment was completed successfully.";
            }

            send(
                    user,
                    "Park Nova - Payment Successful",
                    "Payment Successful",
                    message,
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "payment successful",
                    e
            );
        }
    }

    public void sendPaymentFailed(
            Payment payment) {

        try {

            User user =
                    findUser(
                            payment.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Payment Failed",
                    "Payment Failed",
                    "Your payment could not be completed.",
                    paymentDetails(
                            payment
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "payment failed",
                    e
            );
        }
    }

    // =========================================================
    // REFUND NOTIFICATIONS
    // =========================================================

    public void sendRefundRequested(
            Payment payment) {

        try {

            User user =
                    findUser(
                            payment.getUserId()
                    );

            String content =
                    infoRow(
                            "Payment ID",
                            payment.getId()
                    )
                    + infoRow(
                            "Refund Amount",
                            formatMoney(
                                    payment.getRefundAmount()
                            )
                    )
                    + infoRow(
                            "Refund Destination",
                            formatRefundDestination(
                                    payment.getRefundDestination()
                            )
                    )
                    + infoRow(
                            "Reason",
                            payment.getRefundReason()
                    )
                    + infoRow(
                            "Status",
                            payment.getStatus().name()
                    );

            send(
                    user,
                    "Park Nova - Refund Requested",
                    "Refund Requested",
                    "Your refund request has been received.",
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "refund requested",
                    e
            );
        }
    }

    public void sendRefundCompleted(
            Payment payment,
            Double walletBalance) {

        try {

            User user =
                    findUser(
                            payment.getUserId()
                    );

            boolean walletRefund =
                    payment.getRefundDestination()
                            == RefundDestination.WALLET;

            String message;

            if (walletRefund) {

                message =
                        "Your refund has been credited "
                        + "to your Park Nova Wallet.";

            } else {

                message =
                        "Your refund has been completed successfully.";
            }

            String content =
                    infoRow(
                            "Payment ID",
                            payment.getId()
                    )
                    + infoRow(
                            "Refund Amount",
                            formatMoney(
                                    payment.getRefundAmount()
                            )
                    )
                    + infoRow(
                            "Refund Destination",
                            formatRefundDestination(
                                    payment.getRefundDestination()
                            )
                    );

            if (walletRefund
                    && walletBalance != null) {

                content +=
                        infoRow(
                                "Total Wallet Balance",
                                formatMoney(
                                        walletBalance
                                )
                        );
            }

            content +=
                    infoRow(
                            "Refund Transaction ID",
                            payment.getRefundTransactionId()
                    )
                    + infoRow(
                            "Status",
                            payment.getStatus().name()
                    );

            send(
                    user,
                    "Park Nova - Refund Completed",
                    "Refund Completed",
                    message,
                    content
            );

        } catch (Exception e) {

            logFailure(
                    "refund completed",
                    e
            );
        }
    }

    // =========================================================
    // MONTHLY PASS NOTIFICATIONS
    // =========================================================

    public void sendMonthlyPassCreated(
            MonthlyPass monthlyPass) {

        try {

            User user =
                    findUser(
                            monthlyPass.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Monthly Pass Created",
                    "Monthly Pass Created",
                    "Your monthly pass request has been created. Please complete payment to activate it.",
                    monthlyPassDetails(
                            monthlyPass
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "monthly pass created",
                    e
            );
        }
    }

    public void sendMonthlyPassActivated(
            MonthlyPass monthlyPass) {

        try {

            User user =
                    findUser(
                            monthlyPass.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Monthly Pass Activated",
                    "Monthly Pass Activated",
                    "Your Park Nova monthly pass is now active.",
                    monthlyPassDetails(
                            monthlyPass
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "monthly pass activated",
                    e
            );
        }
    }

    public void sendMonthlyPassPaymentFailed(
            MonthlyPass monthlyPass) {

        try {

            User user =
                    findUser(
                            monthlyPass.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Monthly Pass Payment Failed",
                    "Monthly Pass Payment Failed",
                    "Your monthly pass could not be activated because the payment failed.",
                    monthlyPassDetails(
                            monthlyPass
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "monthly pass payment failed",
                    e
            );
        }
    }

    public void sendMonthlyPassCancelled(
            MonthlyPass monthlyPass) {

        try {

            User user =
                    findUser(
                            monthlyPass.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Monthly Pass Cancelled",
                    "Monthly Pass Cancelled",
                    "Your Park Nova monthly pass has been cancelled.",
                    monthlyPassDetails(
                            monthlyPass
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "monthly pass cancelled",
                    e
            );
        }
    }

    public void sendMonthlyPassExpired(
            MonthlyPass monthlyPass) {

        try {

            User user =
                    findUser(
                            monthlyPass.getUserId()
                    );

            send(
                    user,
                    "Park Nova - Monthly Pass Expired",
                    "Monthly Pass Expired",
                    "Your Park Nova monthly pass has expired.",
                    monthlyPassDetails(
                            monthlyPass
                    )
            );

        } catch (Exception e) {

            logFailure(
                    "monthly pass expired",
                    e
            );
        }
    }

    // =========================================================
    // DETAILS HELPERS
    // =========================================================

    private String bookingDetails(
            Booking booking) {

        return infoRow(
                    "Booking ID",
                    booking.getId()
                )
                + infoRow(
                    "Vehicle",
                    booking.getVehicleNumber()
                )
                + infoRow(
                    "Start Time",
                    formatDateTime(
                            booking.getStartTime()
                    )
                )
                + infoRow(
                    "End Time",
                    formatDateTime(
                            booking.getEndTime()
                    )
                )
                + infoRow(
                    "Amount",
                    formatMoney(
                            booking.getTotalAmount()
                    )
                )
                + infoRow(
                    "Status",
                    booking.getStatus() == null
                            ? "-"
                            : booking.getStatus().name()
                );
    }

    private String monthlyPassDetails(
            MonthlyPass monthlyPass) {

        return infoRow(
                    "Monthly Pass ID",
                    monthlyPass.getId()
                )
                + infoRow(
                    "Vehicle",
                    monthlyPass.getVehicleNumber()
                )
                + infoRow(
                    "Vehicle Type",
                    monthlyPass.getVehicleType() == null
                            ? "-"
                            : monthlyPass.getVehicleType().name()
                )
                + infoRow(
                    "Amount",
                    formatMoney(
                            monthlyPass.getPricePaid()
                    )
                )
                + infoRow(
                    "Start Date",
                    formatDateTime(
                            monthlyPass.getStartDate()
                    )
                )
                + infoRow(
                    "Expiry Date",
                    formatDateTime(
                            monthlyPass.getExpiryDate()
                    )
                )
                + infoRow(
                    "Status",
                    monthlyPass.getStatus() == null
                            ? "-"
                            : monthlyPass.getStatus().name()
                );
    }

    private String paymentDetails(
            Payment payment) {

        return infoRow(
                    "Payment ID",
                    payment.getId()
                )
                + infoRow(
                    "Payment Type",
                    payment.getPaymentType() == null
                            ? "-"
                            : payment.getPaymentType().name()
                )
                + infoRow(
                    "Reference ID",
                    payment.getReferenceId()
                )
                + infoRow(
                    "Amount",
                    formatMoney(
                            payment.getAmount()
                    )
                )
                + infoRow(
                    "Payment Method",
                    payment.getPaymentMethod() == null
                            ? "-"
                            : payment.getPaymentMethod().name()
                )
                + infoRow(
                    "Status",
                    payment.getStatus() == null
                            ? "-"
                            : payment.getStatus().name()
                );
    }

    // =========================================================
    // COMMON HELPERS
    // =========================================================

    private String formatRefundDestination(
            RefundDestination destination) {

        if (destination
                == RefundDestination.WALLET) {

            return "Park Nova Wallet";
        }

        return "Original Payment Method";
    }

    private User findUser(
            String userId) {

        if (userId == null
                || userId.isBlank()) {

            return null;
        }

        return userRepository
                .findById(userId)
                .orElse(null);
    }

    private void send(
            User user,
            String subject,
            String heading,
            String message,
            String details) {

        if (user == null
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            return;
        }

        String userName =
                user.getName() == null
                        || user.getName().isBlank()
                        ? "Customer"
                        : user.getName();

        String html =
                buildTemplate(
                        userName,
                        heading,
                        message,
                        details
                );

        NotificationLog notificationLog =
                new NotificationLog();

        notificationLog.setUserId(
                user.getId()
        );

        notificationLog.setRecipientEmail(
                user.getEmail()
        );

        notificationLog.setType(
                heading == null
                        || heading.isBlank()
                        ? "GENERAL"
                        : heading
        );

        notificationLog.setSubject(
                subject
        );

        notificationLog.setMessage(
                message
        );

        notificationLog.setStatus(
                "PENDING"
        );

        notificationLog.setCreatedAt(
                LocalDateTime.now()
        );

        NotificationLog savedLog =
                notificationLogRepository.save(
                        notificationLog
                );

        try {

            emailService.sendHtmlEmailSafely(
                    user.getEmail(),
                    subject,
                    html
            );

            savedLog.setStatus(
                    "DISPATCHED"
            );

            savedLog.setSentAt(
                    LocalDateTime.now()
            );

        } catch (Exception exception) {

            savedLog.setStatus(
                    "FAILED"
            );

            savedLog.setErrorMessage(
                    exception.getMessage()
            );
        }

        notificationLogRepository.save(
                savedLog
        );
    }

    private String buildTemplate(
            String userName,
            String heading,
            String message,
            String details) {

        return """
                <!DOCTYPE html>
                <html>
                <body style="
                    margin:0;
                    padding:0;
                    background:#071014;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#eafcff;
                ">

                <div style="
                    max-width:620px;
                    margin:30px auto;
                    background:#0d181d;
                    border:1px solid #1bd9c5;
                    border-radius:16px;
                    overflow:hidden;
                ">

                    <div style="
                        padding:28px;
                        text-align:center;
                        background:#091317;
                        border-bottom:1px solid #18383d;
                    ">

                        <div style="
                            font-size:30px;
                            font-weight:700;
                            color:#62ff91;
                        ">
                            PARK NOVA
                        </div>

                        <div style="
                            margin-top:6px;
                            color:#61e8ef;
                            font-size:14px;
                        ">
                            Park Smart Live Better
                        </div>

                    </div>

                    <div style="padding:30px;">

                        <h2 style="
                            margin-top:0;
                            color:#62ff91;
                        ">
                            %s
                        </h2>

                        <p style="
                            color:#c9dfe3;
                            line-height:1.7;
                        ">
                            Hello %s,
                        </p>

                        <p style="
                            color:#c9dfe3;
                            line-height:1.7;
                        ">
                            %s
                        </p>

                        <div style="
                            margin-top:24px;
                            background:#101f24;
                            border:1px solid #21454c;
                            border-radius:12px;
                            padding:18px;
                        ">
                            %s
                        </div>

                        <p style="
                            margin-top:28px;
                            color:#819ca1;
                            font-size:13px;
                            line-height:1.6;
                        ">
                            This is an automated notification
                            from Park Nova.
                        </p>

                    </div>

                    <div style="
                        padding:18px;
                        text-align:center;
                        background:#081115;
                        color:#607b80;
                        font-size:12px;
                    ">
                        Park Nova &bull; Smart Parking System
                    </div>

                </div>

                </body>
                </html>
                """.formatted(
                        escapeHtml(heading),
                        escapeHtml(userName),
                        escapeHtml(message),
                        details == null ? "" : details
                );
    }

    private String infoRow(
            String label,
            String value) {

        return """
                <div style="
                    margin:8px 0;
                    line-height:1.6;
                ">
                    <span style="
                        color:#7f9da2;
                    ">
                        %s:
                    </span>

                    <strong style="
                        color:#eaffff;
                    ">
                        %s
                    </strong>
                </div>
                """.formatted(
                        escapeHtml(label),
                        escapeHtml(value)
                );
    }

    private String formatMoney(
            double amount) {

        return String.format(
                "₹%.2f",
                amount
        );
    }

    private String formatDateTime(
            LocalDateTime dateTime) {

        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(
                DATE_TIME_FORMAT
        );
    }

    private String escapeHtml(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void logFailure(
            String notificationType,
            Exception exception) {

        System.err.println(
                "Park Nova notification failed ["
                + notificationType
                + "]: "
                + exception.getMessage()
        );
    }
}