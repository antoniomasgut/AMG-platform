package com.amg.digitalitzacio.agents.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
    public boolean isConfigured() {
        String json = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        return json != null && !json.isBlank();
    }

    /** Crea un event al calendari del tenant quan el bot confirma una cita. */
    public void createEvent(String calendarId, String title,
                             LocalDateTime start, int durationMinutes,
                             String description) {
        if (calendarId == null || calendarId.isBlank()) return;
        String saJson = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        if (saJson == null || saJson.isBlank()) {
            log.debug("GOOGLE_CALENDAR_SA_JSON not configured — skipping calendar event");
            return;
        }
        try {
            Map<String, Object> sa = objectMapper.readValue(saJson, new TypeReference<>() {});
            String clientEmail = (String) sa.get("client_email");
            String privateKeyPem = (String) sa.get("private_key");

            String accessToken = getAccessToken(clientEmail, privateKeyPem);
            LocalDateTime end = start.plusMinutes(durationMinutes);

            String eventJson = buildEventJson(title, start, end, description);
            String encodedId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);

            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/" + encodedId + "/events"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(eventJson))
                    .build();

            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.info("Google Calendar event created: calendarId={}, title={}", calendarId, title);
            } else {
                log.error("Google Calendar API error {}: {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.error("Failed to create Google Calendar event for calendarId={}", calendarId, e);
        }
    }

    private String getAccessToken(String clientEmail, String privateKeyPem) throws Exception {
        long now = Instant.now().getEpochSecond();

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        String claimsJson = objectMapper.writeValueAsString(Map.of(
                "iss", clientEmail,
                "scope", CALENDAR_SCOPE,
                "aud", TOKEN_URL,
                "exp", now + 3600,
                "iat", now
        ));
        String claims = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));

        String toSign = header + "." + claims;
        byte[] sig = signRS256(toSign.getBytes(StandardCharsets.UTF_8), privateKeyPem);
        String jwt = toSign + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sig);

        String body = "grant_type=" + URLEncoder.encode(
                "urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + jwt;

        var req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = objectMapper.readValue(res.body(), new TypeReference<>() {});
        String token = (String) resp.get("access_token");
        if (token == null) throw new IllegalStateException("No access_token in response: " + res.body());
        return token;
    }

    private byte[] signRS256(byte[] data, String pem) throws Exception {
        String keyContent = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pk);
        sig.update(data);
        return sig.sign();
    }

    private String buildEventJson(String title, LocalDateTime start, LocalDateTime end, String description) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return """
                {
                  "summary": %s,
                  "description": %s,
                  "start": {"dateTime": "%s", "timeZone": "Europe/Madrid"},
                  "end":   {"dateTime": "%s", "timeZone": "Europe/Madrid"}
                }
                """.formatted(
                jsonString(title),
                jsonString(description != null ? description : ""),
                start.format(fmt),
                end.format(fmt)
        );
    }

    private String jsonString(String s) {
        try { return objectMapper.writeValueAsString(s); }
        catch (Exception e) { return "\"" + s.replace("\"", "'") + "\""; }
    }
}
