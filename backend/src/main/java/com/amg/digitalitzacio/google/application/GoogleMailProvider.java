package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.shared.mail.MailProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Slf4j
public class GoogleMailProvider implements MailProvider {

    private final String accessToken;
    private final String userEmail;
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public GoogleMailProvider(String accessToken, String userEmail) {
        this.accessToken = accessToken;
        this.userEmail = userEmail;
    }

    @Override
    public void send(String to, String subject, String body, String attachmentName, InputStream attachmentData, String attachmentMimeType) {
        try {
            var emailContent = buildEmailContent(to, subject, body);
            var encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(emailContent.getBytes("UTF-8"));

            var json = new JsonObject();
            json.addProperty("raw", encoded);

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Gmail send failed: " + response.statusCode() + " " + response.body());
            }
            log.info("Email sent via Gmail: to={}, subject={}", to, subject);
        } catch (Exception e) {
            throw new RuntimeException("Gmail send failed: " + e.getMessage(), e);
        }
    }

    private String buildEmailContent(String to, String subject, String body) {
        return "From: " + userEmail + "\r\n"
            + "To: " + to + "\r\n"
            + "Subject: =?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "?=\r\n"
            + "MIME-Version: 1.0\r\n"
            + "Content-Type: text/plain; charset=\"UTF-8\"\r\n"
            + "Content-Transfer-Encoding: base64\r\n"
            + "\r\n"
            + Base64.getEncoder().encodeToString(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public String getProviderName() { return "google_gmail"; }
}
