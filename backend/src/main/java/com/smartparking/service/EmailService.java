package com.smartparking.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /*
     * Existing plain-text method.
     *
     * Keep this method because email verification
     * and password reset may already use it.
     */
    public void sendEmail(
            String to,
            String subject,
            String body) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    /*
     * Professional HTML email.
     */
    public void sendHtmlEmail(
            String to,
            String subject,
            String htmlBody) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            message.setFrom(fromEmail);
            message.setRecipients(
                    MimeMessage.RecipientType.TO,
                    to
            );

            message.setSubject(
                    subject,
                    StandardCharsets.UTF_8.name()
            );

            message.setContent(
                    htmlBody,
                    "text/html; charset=UTF-8"
            );

            mailSender.send(message);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Unable to send email",
                    exception
            );
        }
    }

    /*
     * Notification emails must never make a
     * successful booking/payment fail.
     */
    public void sendHtmlEmailSafely(
            String to,
            String subject,
            String htmlBody) {

        try {

            sendHtmlEmail(
                    to,
                    subject,
                    htmlBody
            );

        } catch (Exception exception) {

            System.err.println(
                    "Park Nova email notification failed: "
                            + exception.getMessage()
            );
        }
    }

    // Existing temporary configuration helper
    public String getFromEmail() {
        return fromEmail;
    }
}