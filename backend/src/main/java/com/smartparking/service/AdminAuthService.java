package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartparking.dto.LoginRequest;
import com.smartparking.exception.BadRequestException;
import com.smartparking.model.AdminLoginApproval;
import com.smartparking.model.User;
import com.smartparking.repository.AdminLoginApprovalRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.security.JwtService;

@Service
public class AdminAuthService {

    private static final int APPROVAL_MINUTES = 10;

    private final UserRepository userRepository;
    private final AdminLoginApprovalRepository approvalRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AdminAuthService(
            UserRepository userRepository,
            AdminLoginApprovalRepository approvalRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.approvalRepository = approvalRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    public Map<String, Object> beginLogin(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Invalid email or password"
                        )
                );

        String role = normalizeRole(user.getRole());

        if (!"ADMIN".equals(role)
                || user.getPassword() == null
                || user.getPassword().isBlank()
                || !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                )) {

            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        if (!user.isEnabled()) {
            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        if (!user.isEmailVerified()
                && "LOCAL".equalsIgnoreCase(
                        user.getAuthProvider()
                )) {

            throw new BadRequestException(
                    "Please verify your email before login"
            );
        }

        AdminLoginApproval approval =
                new AdminLoginApproval();

        approval.setRequestId(
                UUID.randomUUID().toString()
        );

        approval.setPollToken(
                UUID.randomUUID().toString()
        );

        approval.setActionToken(
                UUID.randomUUID().toString()
        );

        approval.setUserId(user.getId());
        approval.setEmail(user.getEmail());
        approval.setStatus("PENDING");
        approval.setCreatedAt(LocalDateTime.now());
        approval.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(APPROVAL_MINUTES)
        );

        approvalRepository.save(approval);

        String approveLink =
                frontendUrl
                        + "/admin/verify-login?action=approve&token="
                        + approval.getActionToken();

        String denyLink =
                frontendUrl
                        + "/admin/verify-login?action=deny&token="
                        + approval.getActionToken();

        String html =
                "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;"
                + "background:#0f172a;color:#ffffff;padding:32px;border-radius:18px'>"
                + "<h1 style='color:#00e676'>Park Nova Admin Sign-In</h1>"
                + "<p>Hello " + escapeHtml(user.getName()) + ",</p>"
                + "<p>A sign-in attempt was made for your Park Nova admin account.</p>"
                + "<p>Please confirm whether this login is yours.</p>"
                + "<div style='margin:30px 0'>"
                + "<a href='" + approveLink + "' "
                + "style='display:inline-block;background:#00e676;color:#07111f;"
                + "padding:14px 22px;text-decoration:none;border-radius:10px;"
                + "font-weight:bold;margin-right:10px'>I am Login</a>"
                + "<a href='" + denyLink + "' "
                + "style='display:inline-block;background:#ef4444;color:white;"
                + "padding:14px 22px;text-decoration:none;border-radius:10px;"
                + "font-weight:bold'>I am Not Login</a>"
                + "</div>"
                + "<p>This request expires in "
                + APPROVAL_MINUTES
                + " minutes.</p>"
                + "<p>If this was not you, choose <b>I am Not Login</b>.</p>"
                + "<p style='color:#94a3b8'>Park Smart Live Better<br>Park Nova</p>"
                + "</div>";

        try {
            emailService.sendHtmlEmail(
                    user.getEmail(),
                    "Approve Admin Sign-In - Park Nova",
                    html
            );
        } catch (RuntimeException exception) {

            approvalRepository.delete(approval);

            System.err.println(
                    "ADMIN APPROVAL EMAIL FAILED: "
                            + exception.getClass().getName()
                            + ": "
                            + exception.getMessage()
            );

            Throwable cause = exception.getCause();

            while (cause != null) {
                System.err.println(
                        "CAUSED BY: "
                                + cause.getClass().getName()
                                + ": "
                                + cause.getMessage()
                );

                cause = cause.getCause();
            }

            throw new BadRequestException(
                    "Unable to send admin approval email. Please try again."
            );
        }

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                "Approval email sent. Confirm the login from your admin email."
        );

        response.put(
                "requestId",
                approval.getRequestId()
        );

        response.put(
                "pollToken",
                approval.getPollToken()
        );

        response.put(
                "expiresInSeconds",
                APPROVAL_MINUTES * 60
        );

        return response;
    }

    public Map<String, Object> decide(
            String actionToken,
            String action) {

        if (actionToken == null
                || actionToken.isBlank()) {

            throw new BadRequestException(
                    "Invalid admin login approval"
            );
        }

        AdminLoginApproval approval =
                approvalRepository
                        .findByActionToken(
                                actionToken.trim()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid admin login approval"
                                )
                        );

        if (!"PENDING".equals(
                approval.getStatus()
        )) {

            throw new BadRequestException(
                    "This admin login request has already been handled"
            );
        }

        if (approval.getExpiresAt() == null
                || approval.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            approval.setStatus("EXPIRED");
            approval.setDecidedAt(LocalDateTime.now());
            approvalRepository.save(approval);

            throw new BadRequestException(
                    "This admin login request has expired"
            );
        }

        if ("approve".equalsIgnoreCase(action)) {

            approval.setStatus("APPROVED");

        } else if ("deny".equalsIgnoreCase(action)) {

            approval.setStatus("DENIED");

        } else {

            throw new BadRequestException(
                    "Invalid admin login action"
            );
        }

        approval.setDecidedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "status",
                approval.getStatus()
        );

        response.put(
                "message",
                "APPROVED".equals(approval.getStatus())
                        ? "Admin login approved. You can return to the original browser."
                        : "Admin login denied."
        );

        return response;
    }

    public Map<String, Object> poll(
            String requestId,
            String pollToken) {

        if (requestId == null
                || requestId.isBlank()
                || pollToken == null
                || pollToken.isBlank()) {

            throw new BadRequestException(
                    "Invalid admin login request"
            );
        }

        AdminLoginApproval approval =
                approvalRepository
                        .findByRequestIdAndPollToken(
                                requestId.trim(),
                                pollToken.trim()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid admin login request"
                                )
                        );

        if (approval.getExpiresAt() == null
                || approval.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            if ("PENDING".equals(approval.getStatus())
                    || "APPROVED".equals(approval.getStatus())) {

                approval.setStatus("EXPIRED");
                approval.setDecidedAt(LocalDateTime.now());
                approvalRepository.save(approval);
            }

            return statusResponse("EXPIRED");
        }

        if ("PENDING".equals(approval.getStatus())) {
            return statusResponse("PENDING");
        }

        if ("DENIED".equals(approval.getStatus())) {
            return statusResponse("DENIED");
        }

        if ("CONSUMED".equals(approval.getStatus())) {
            return statusResponse("CONSUMED");
        }

        if (!"APPROVED".equals(approval.getStatus())) {
            return statusResponse(
                    approval.getStatus()
            );
        }

        User user =
                userRepository
                        .findById(
                                approval.getUserId()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Admin account is unavailable"
                                )
                        );

        if (!user.isEnabled()
                || !"ADMIN".equals(
                        normalizeRole(user.getRole())
                )) {

            approval.setStatus("DENIED");
            approvalRepository.save(approval);

            throw new BadRequestException(
                    "Admin account is unavailable"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        "ADMIN"
                );

        /*
         * The approval can issue a JWT only once.
         */
        approval.setStatus("CONSUMED");
        approval.setActionToken(null);
        approval.setPollToken(null);
        approval.setDecidedAt(LocalDateTime.now());

        approvalRepository.save(approval);

        Map<String, Object> response =
                new HashMap<>();

        response.put("status", "APPROVED");
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", "ADMIN");
        response.put(
                "emailVerified",
                user.isEmailVerified()
        );

        return response;
    }

    private Map<String, Object> statusResponse(
            String status) {

        Map<String, Object> response =
                new HashMap<>();

        response.put("status", status);

        return response;
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        return email.trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            return "USER";
        }

        String normalized =
                role.trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            normalized =
                    normalized.substring(5);
        }

        return "ADMIN".equals(normalized)
                ? "ADMIN"
                : "USER";
    }

    private String escapeHtml(String value) {

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
}