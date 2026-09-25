package com.smartparking.service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final URI TOKEN_URL =
            URI.create("https://oauth2.googleapis.com/token");

    private static final URI SEND_URL =
            URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper;

    @Value("${MAIL_USERNAME:}")
    private String fromEmail;

    @Value("${GOOGLE_MAIL_CLIENT_ID:}")
    private String clientId;

    @Value("${GOOGLE_MAIL_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${GOOGLE_MAIL_REFRESH_TOKEN:}")
    private String refreshToken;

    public EmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendEmail(String to, String subject, String body) {
        send(to, subject, body, "text/plain; charset=UTF-8");
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        send(to, subject, htmlBody, "text/html; charset=UTF-8");
    }

    public void sendHtmlEmailSafely(String to, String subject, String htmlBody) {
        try {
            sendHtmlEmail(to, subject, htmlBody);
        } catch (Exception exception) {
            System.err.println("Park Nova email notification failed: "
                    + exception.getMessage());
        }
    }

    public String getFromEmail() {
        return fromEmail;
    }

    private void send(String to, String subject, String content, String contentType) {
        if (isBlank(fromEmail) || isBlank(clientId)
                || isBlank(clientSecret) || isBlank(refreshToken)) {
            throw new IllegalStateException(
                    "Gmail API configuration is incomplete");
        }

        try {
            String accessToken = requestAccessToken();

            MimeMessage message = new MimeMessage(
                    Session.getInstance(new Properties()));
            message.setFrom(new InternetAddress(fromEmail, true));
            message.setRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(to, true));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setContent(content, contentType);
            message.saveChanges();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            message.writeTo(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(bytes.toByteArray());

            String json = objectMapper.createObjectNode()
                    .put("raw", raw)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder(SEND_URL)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Gmail send failed (HTTP " + response.statusCode() + ")");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gmail send interrupted", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException) {
                throw (IllegalStateException) exception;
            }
            throw new IllegalStateException("Unable to send email via Gmail API",
                    exception);
        }
    }

    private String requestAccessToken() throws Exception {
        String form = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder(TOKEN_URL)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Gmail token request failed (HTTP "
                            + response.statusCode() + ")");
        }

        JsonNode token = objectMapper.readTree(response.body());
        String accessToken = token.path("access_token").asText();

        if (isBlank(accessToken)) {
            throw new IllegalStateException(
                    "Gmail token response has no access token");
        }

        return accessToken;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}