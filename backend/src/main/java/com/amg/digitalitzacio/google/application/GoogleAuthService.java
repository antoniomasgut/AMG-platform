package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.domain.OAuthState;
import com.amg.digitalitzacio.google.domain.OAuthStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final int STATE_EXPIRY_MINUTES = 10;

    private static final Map<String, String> SCOPES = Map.of(
        "drive", "https://www.googleapis.com/auth/drive.file",
        "gmail", "https://www.googleapis.com/auth/gmail.send",
        "calendar", "https://www.googleapis.com/auth/calendar",
        "sheets", "https://www.googleapis.com/auth/spreadsheets"
    );

    private final OAuthStateRepository stateRepo;
    private final GoogleConfigService configService;
    private final GoogleTokenService tokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newHttpClient();

    public record AuthUrlResponse(String authUrl, String stateToken) {}
    public record TokenResponse(String accessToken, String refreshToken, int expiresIn, String email, String googleUserId) {}

    @Transactional
    public AuthUrlResponse generateAuthUrl(UUID tenantId, java.util.List<String> modules, String redirectUri) {
        var scopes = modules.stream()
            .map(m -> SCOPES.getOrDefault(m, SCOPES.get("drive")))
            .collect(Collectors.joining(" "));

        var state = new OAuthState();
        state.setTenantId(tenantId);
        state.setStateToken(UUID.randomUUID().toString().replace("-", ""));
        state.setRedirectUri(redirectUri);
        state.setRequestedScopes(scopes);
        state.setExpiresAt(Instant.now().plus(STATE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        stateRepo.save(state);

        var clientId = configService.getClientId();
        var params = "client_id=" + urlEncode(clientId)
            + "&redirect_uri=" + urlEncode(redirectUri)
            + "&response_type=code"
            + "&scope=" + urlEncode(scopes)
            + "&access_type=offline"
            + "&prompt=consent"
            + "&state=" + urlEncode(state.getStateToken());

        return new AuthUrlResponse(AUTH_URL + "?" + params, state.getStateToken());
    }

    @Transactional
    public TokenResponse handleCallback(String code, String stateToken) {
        var state = stateRepo.findByStateToken(stateToken)
            .orElseThrow(() -> new RuntimeException("State invàlid o inexistent"));

        if (state.isExpired()) {
            stateRepo.delete(state);
            throw new RuntimeException("State expirat");
        }

        stateRepo.delete(state);

        try {
            var clientId = configService.getClientId();
            var clientSecret = configService.getClientSecret();

            var body = "code=" + urlEncode(code)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&redirect_uri=" + urlEncode(state.getRedirectUri())
                + "&grant_type=authorization_code";

            var tokenReq = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            var tokenRes = http.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            if (tokenRes.statusCode() != 200) {
                throw new RuntimeException("Error intercanviant code: " + tokenRes.body());
            }

            @SuppressWarnings("unchecked")
            var tokenJson = objectMapper.readValue(tokenRes.body(), Map.class);
            var accessToken = (String) tokenJson.get("access_token");
            var refreshToken = (String) tokenJson.get("refresh_token");
            var expiresIn = ((Number) tokenJson.getOrDefault("expires_in", 3600)).intValue();

            var userReq = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            var userRes = http.send(userReq, HttpResponse.BodyHandlers.ofString());
            @SuppressWarnings("unchecked")
            var userJson = objectMapper.readValue(userRes.body(), Map.class);
            var email = (String) userJson.get("email");
            var userId = (String) userJson.get("id");

            var tenantId = state.getTenantId();
            tokenService.saveTokens(tenantId, accessToken, refreshToken, expiresIn, email, userId);

            return new TokenResponse(accessToken, refreshToken, expiresIn, email, userId);

        } catch (Exception e) {
            throw new RuntimeException("Error al callback OAuth: " + e.getMessage(), e);
        }
    }

    public static String buildScopeString(java.util.List<String> modules) {
        return modules.stream()
            .map(m -> SCOPES.getOrDefault(m, SCOPES.get("drive")))
            .collect(Collectors.joining(" "));
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
