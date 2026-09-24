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
import com.smartparking.dto.RegisterRequest;
import com.smartparking.dto.ResetPasswordRequest;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ConflictException;
import com.smartparking.exception.NotFoundException;
import com.smartparking.model.PasswordResetToken;
import com.smartparking.model.User;
import com.smartparking.model.VerificationToken;
import com.smartparking.repository.PasswordResetTokenRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.VerificationTokenRepository;
import com.smartparking.security.JwtService;

@Service
public class AuthService {

    private static final String GENERIC_FORGOT_MESSAGE =
            "If an account exists for this email, a password reset link has been sent.";

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    // =========================================================
    // REGISTER
    // =========================================================
    public User register(RegisterRequest request) {

        String email = normalizeEmail(
                request.getEmail()
        );

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                    "Email is already registered"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(
                        request.getPassword()
                );

        User user = new User(
                request.getName().trim(),
                email,
                request.getPhone().trim(),
                hashedPassword
        );

        user.setRole("USER");
        user.setAuthProvider("LOCAL");
        user.setEmailVerified(false);
        user.setEnabled(true);

        User savedUser =
                userRepository.save(user);

        verificationTokenRepository
                .deleteByUserId(savedUser.getId());

        String token =
                UUID.randomUUID().toString();

        VerificationToken verificationToken =
                new VerificationToken(
                        token,
                        savedUser.getId(),
                        LocalDateTime.now()
                                .plusHours(24)
                );

        verificationTokenRepository.save(
                verificationToken
        );

        String verificationLink =
                frontendUrl
                        + "/verify-email?token="
                        + token;

        String emailBody =
                "Hello " + savedUser.getName() + ",\n\n"
                        + "Welcome to Park Nova.\n\n"
                        + "Please verify your email address using the link below:\n\n"
                        + verificationLink + "\n\n"
                        + "This verification link will expire in 24 hours.\n\n"
                        + "If you did not create this account, you can ignore this email.\n\n"
                        + "Park Nova";

        try {

            emailService.sendEmail(
                    savedUser.getEmail(),
                    "Verify Your Email - Park Nova",
                    emailBody
            );

        } catch (Exception e) {

            System.err.println(
                    "Verification email could not be sent: "
                            + e.getClass()
                                    .getSimpleName()
            );
        }

        return savedUser;
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public Map<String, Object> login(
            LoginRequest request) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid email or password"
                                )
                        );

        /*
         * Google-only accounts can have no local password.
         * Never pass null to BCrypt.
         */
        if (user.getPassword() == null
                || user.getPassword().isBlank()
                || !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword())) {

            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        if (!user.isEnabled()) {

            throw new BadRequestException(
                    "Account is disabled"
            );
        }

        if (!user.isEmailVerified()
                && "LOCAL".equalsIgnoreCase(
                        user.getAuthProvider())) {

            throw new BadRequestException(
                    "Please verify your email before login"
            );
        }

        String normalizedRole =
                normalizeRole(
                        user.getRole()
                );

        /*
         * Keep stored role normalized as well.
         */
        if (!normalizedRole.equals(
                user.getRole())) {

            user.setRole(
                    normalizedRole
            );

            user.setUpdatedAt(
                    LocalDateTime.now()
            );

            userRepository.save(
                    user
            );
        }

        /*
         * Public customer login is USER-only.
         * ADMIN accounts must use /api/auth/admin/login
         * and complete email approval.
         */
        if ("ADMIN".equals(normalizedRole)) {
            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        "USER"
                );

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                "Login successful"
        );

        response.put(
                "token",
                token
        );

        response.put(
                "userId",
                user.getId()
        );

        response.put(
                "name",
                user.getName()
        );

        response.put(
                "email",
                user.getEmail()
        );

        response.put(
                "role",
                normalizedRole
        );

        response.put(
                "emailVerified",
                user.isEmailVerified()
        );

        return response;
    }

    // =========================================================
    // VERIFY EMAIL
    // =========================================================
    public Map<String, Object> verifyEmail(
            String token) {

        if (token == null
                || token.isBlank()) {

            throw new BadRequestException(
                    "Invalid verification token"
            );
        }

        VerificationToken verificationToken =
                verificationTokenRepository
                        .findByToken(token.trim())
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid verification token"
                                )
                        );

        if (verificationToken
                .getExpiryDate()
                .isBefore(
                        LocalDateTime.now()
                )) {

            verificationTokenRepository.delete(
                    verificationToken
            );

            throw new BadRequestException(
                    "Verification token has expired"
            );
        }

        User user =
                userRepository
                        .findById(
                                verificationToken
                                        .getUserId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        if (!user.isEnabled()) {

            throw new BadRequestException(
                    "Account is disabled"
            );
        }

        user.setEmailVerified(true);

        user.setUpdatedAt(
                LocalDateTime.now()
        );

        userRepository.save(
                user
        );

        verificationTokenRepository.delete(
                verificationToken
        );

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                "Email verified successfully"
        );

        response.put(
                "email",
                user.getEmail()
        );

        response.put(
                "emailVerified",
                true
        );

        return response;
    }

    // =========================================================
    // RESEND VERIFICATION EMAIL
    // =========================================================
    public Map<String, Object> resendVerificationEmail(
            String email) {

        String normalizedEmail =
                normalizeEmail(email);

        User user =
                userRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        if (!user.isEnabled()) {

            throw new BadRequestException(
                    "Account is disabled"
            );
        }

        if (user.isEmailVerified()) {

            throw new ConflictException(
                    "Email is already verified"
            );
        }

        verificationTokenRepository
                .deleteByUserId(
                        user.getId()
                );

        String token =
                UUID.randomUUID().toString();

        VerificationToken verificationToken =
                new VerificationToken(
                        token,
                        user.getId(),
                        LocalDateTime.now()
                                .plusHours(24)
                );

        verificationTokenRepository.save(
                verificationToken
        );

        String verificationLink =
                frontendUrl
                        + "/verify-email?token="
                        + token;

        String emailBody =
                "Hello " + user.getName() + ",\n\n"
                        + "Here is your new Park Nova email verification link:\n\n"
                        + verificationLink + "\n\n"
                        + "This link will expire in 24 hours.\n\n"
                        + "Park Nova";

        try {

            emailService.sendEmail(
                    user.getEmail(),
                    "Verify Your Email - Park Nova",
                    emailBody
            );

        } catch (Exception e) {

            System.err.println(
                    "Verification email could not be sent: "
                            + e.getClass()
                                    .getSimpleName()
            );
        }

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                "Verification email sent successfully"
        );

        return response;
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================
    public Map<String, Object> forgotPassword(
            String email) {

        String normalizedEmail =
                normalizeEmail(email);

        User user =
                userRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElse(null);

        /*
         * Same public response for:
         * - unknown account
         * - Google-only account
         * - disabled account
         *
         * This prevents account enumeration.
         */
        if (user == null
                || !user.isEnabled()
                || user.getPassword() == null
                || user.getPassword().isBlank()) {

            return genericForgotResponse();
        }

        passwordResetTokenRepository
                .deleteByUserId(
                        user.getId()
                );

        String token =
                UUID.randomUUID().toString();

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        token,
                        user.getId(),
                        LocalDateTime.now()
                                .plusMinutes(15)
                );

        passwordResetTokenRepository.save(
                resetToken
        );

        String resetLink =
                frontendUrl
                        + "/reset-password?token="
                        + token;

        String emailBody =
                "Hello " + user.getName() + ",\n\n"
                        + "We received a request to reset your Park Nova password.\n\n"
                        + "Use the link below to reset your password:\n\n"
                        + resetLink + "\n\n"
                        + "This link will expire in 15 minutes.\n\n"
                        + "If you did not request this password reset, you can ignore this email.\n\n"
                        + "Park Nova";

        try {

            emailService.sendEmail(
                    user.getEmail(),
                    "Reset Your Password - Park Nova",
                    emailBody
            );

        } catch (Exception e) {

            System.err.println(
                    "Password reset email could not be sent: "
                            + e.getClass()
                                    .getSimpleName()
            );
        }

        return genericForgotResponse();
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================
    public Map<String, Object> resetPassword(
            ResetPasswordRequest request) {

        if (request.getToken() == null
                || request.getToken().isBlank()) {

            throw new BadRequestException(
                    "Invalid password reset token"
            );
        }

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(
                                request.getToken()
                                        .trim()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid password reset token"
                                )
                        );

        if (resetToken
                .getExpiryDate()
                .isBefore(
                        LocalDateTime.now()
                )) {

            passwordResetTokenRepository.delete(
                    resetToken
            );

            throw new BadRequestException(
                    "Password reset token has expired"
            );
        }

        User user =
                userRepository
                        .findById(
                                resetToken
                                        .getUserId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "User not found"
                                )
                        );

        if (!user.isEnabled()) {

            passwordResetTokenRepository.delete(
                    resetToken
            );

            throw new BadRequestException(
                    "Account is disabled"
            );
        }

        /*
         * Password reset is intended for LOCAL
         * credential accounts.
         */
        if (user.getPassword() == null
                || user.getPassword().isBlank()) {

            passwordResetTokenRepository.delete(
                    resetToken
            );

            throw new BadRequestException(
                    "Password reset is unavailable for this account"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(
                        request.getNewPassword()
                );

        user.setPassword(
                hashedPassword
        );

        user.setUpdatedAt(
                LocalDateTime.now()
        );

        userRepository.save(
                user
        );

        /*
         * One-time token.
         */
        passwordResetTokenRepository.delete(
                resetToken
        );

        try {

            emailService.sendEmail(
                    user.getEmail(),
                    "Password Changed - Park Nova",
                    "Hello " + user.getName() + ",\n\n"
                            + "Your Park Nova password was changed successfully.\n\n"
                            + "If you did not make this change, please contact support.\n\n"
                            + "Park Nova"
            );

        } catch (Exception e) {

            System.err.println(
                    "Password changed email could not be sent: "
                            + e.getClass()
                                    .getSimpleName()
            );
        }

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                "Password reset successfully"
        );

        return response;
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String normalizeEmail(
            String email) {

        if (email == null
                || email.isBlank()) {

            throw new BadRequestException(
                    "Email is required"
            );
        }

        return email.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeRole(
            String role) {

        if (role == null
                || role.isBlank()) {

            return "USER";
        }

        String normalized =
                role.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (normalized.startsWith(
                "ROLE_")) {

            normalized =
                    normalized.substring(5);
        }

        return "ADMIN".equals(normalized)
                ? "ADMIN"
                : "USER";
    }

    private Map<String, Object>
    genericForgotResponse() {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                GENERIC_FORGOT_MESSAGE
        );

        return response;
    }
}