package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.domain.GoogleConnection;
import com.amg.digitalitzacio.google.domain.GoogleConnectionRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleTokenService {

    private final GoogleConnectionRepository connectionRepo;
    private final VaultEncryption vault;
    private final GoogleConfigService configService;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newHttpClient();

    public record GoogleCredentials(String accessToken, String email) {}

    public GoogleCredentials getValidCredentials(UUID tenantId) {
        var conn = connectionRepo.findByTenantIdAndActiveTrue(tenantId)
            .orElseThrow(() -> new RuntimeException("Google no connectat per aquest tenant"));

        if (conn.getTokenExpiresAt().isBefore(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            conn = refreshTokens(conn);
        }

        var accessToken = vault.decrypt(conn.getEncryptedAccessToken());
        return new GoogleCredentials(accessToken, conn.getGoogleAccountEmail());
    }

    @Transactional
    public GoogleConnection refreshTokens(GoogleConnection conn) {
        try {
            var refreshToken = vault.decrypt(conn.getEncryptedRefreshToken());
            var clientId = configService.getClientId();
            var clientSecret = configService.getClientSecret();

            var body = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                + "&client_secret=" + java.net.URLEncoder.encode(clientSecret, "UTF-8")
                + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8")
                + "&grant_type=refresh_token";

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Token refresh failed for tenant {}: {}", conn.getTenantId(), response.body());
                return conn;
            }

            @SuppressWarnings("unchecked")
            var json = objectMapper.readValue(response.body(), Map.class);
            var newAccessToken = (String) json.get("access_token");
            var expiresIn = ((Number) json.getOrDefault("expires_in", 3600)).intValue();

            conn.setEncryptedAccessToken(vault.encrypt(newAccessToken));
            conn.setTokenExpiresAt(Instant.now().plus(expiresIn - 60, ChronoUnit.SECONDS));
            return connectionRepo.save(conn);

        } catch (Exception e) {
            log.error("Token refresh error for tenant {}: {}", conn.getTenantId(), e.getMessage());
            return conn;
        }
    }

    @Transactional
    public GoogleConnection saveTokens(UUID tenantId, String accessToken, String refreshToken, int expiresIn, String email, String googleUserId) {
        var conn = connectionRepo.findByTenantId(tenantId).orElseGet(() -> {
            var c = new GoogleConnection();
            c.setTenantId(tenantId);
            return c;
        });

        conn.setGoogleAccountEmail(email);
        conn.setGoogleUserId(googleUserId);
        conn.setEncryptedAccessToken(vault.encrypt(accessToken));
        conn.setEncryptedRefreshToken(vault.encrypt(refreshToken));
        conn.setTokenExpiresAt(Instant.now().plus(expiresIn - 60, ChronoUnit.SECONDS));
        conn.setActive(true);
        return connectionRepo.save(conn);
    }

    @Transactional
    public void revoke(UUID tenantId) {
        connectionRepo.findByTenantIdAndActiveTrue(tenantId).ifPresent(conn -> {
            try {
                var token = vault.decrypt(conn.getEncryptedAccessToken());
                var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/revoke?token=" + java.net.URLEncoder.encode(token, "UTF-8")))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
                http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                log.warn("Revoke failed for tenant {}: {}", tenantId, e.getMessage());
            }
            conn.setActive(false);
            connectionRepo.save(conn);
        });
    }
}
