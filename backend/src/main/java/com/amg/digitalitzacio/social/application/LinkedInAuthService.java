package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.social.domain.LinkedInConnection;
import com.amg.digitalitzacio.social.domain.LinkedInConnectionRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
import java.util.concurrent.TimeUnit;

/**
 * OAuth LinkedIn (Mòdul 56 F4) — perfil personal, scope w_member_social.
 * Estat OAuth desat a Redis (TTL 10 min). Token xifrat amb el Vault.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInAuthService {

    private static final String AUTH_URL  = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USERINFO  = "https://api.linkedin.com/v2/userinfo";
    private static final String SCOPES    = "openid profile w_member_social";
    private static final String STATE_KEY = "linkedin:oauth:%s";
    private static final int STATE_TTL_MIN = 10;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final SystemConfigService sysConfig;
    private final LinkedInConnectionRepository connectionRepo;
    private final VaultEncryption vaultEncryption;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public record AuthUrlResponse(String authUrl) {}
    public record ConnectedResponse(String displayName, String personUrn) {}

    /** Genera la URL d'autorització i desa l'estat (tenantId) a Redis */
    public AuthUrlResponse generateAuthUrl(UUID tenantId) {
        String clientId = required("LINKEDIN_CLIENT_ID");
        String redirectUri = required("LINKEDIN_REDIRECT_URI");
        String state = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(STATE_KEY.formatted(state), tenantId.toString(), STATE_TTL_MIN, TimeUnit.MINUTES);

        String params = "response_type=code"
            + "&client_id=" + enc(clientId)
            + "&redirect_uri=" + enc(redirectUri)
            + "&scope=" + enc(SCOPES)
            + "&state=" + enc(state);
        return new AuthUrlResponse(AUTH_URL + "?" + params);
    }

    /** Processa el callback: intercanvia el code, obté el person URN i desa la connexió xifrada */
    public ConnectedResponse handleCallback(String code, String state) {
        String key = STATE_KEY.formatted(state);
        String tenantIdStr = redis.opsForValue().get(key);
        if (tenantIdStr == null) throw new RuntimeException("State LinkedIn invàlid o expirat");
        redis.delete(key);
        UUID tenantId = UUID.fromString(tenantIdStr);

        String clientId = required("LINKEDIN_CLIENT_ID");
        String clientSecret = required("LINKEDIN_CLIENT_SECRET");
        String redirectUri = required("LINKEDIN_REDIRECT_URI");

        try {
            String body = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

            var tokenReq = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            var tokenRes = HTTP.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            if (tokenRes.statusCode() != 200) {
                throw new RuntimeException("Error intercanviant code LinkedIn: " + tokenRes.body());
            }
            @SuppressWarnings("unchecked")
            var tokenJson = objectMapper.readValue(tokenRes.body(), Map.class);
            String accessToken = (String) tokenJson.get("access_token");
            int expiresIn = ((Number) tokenJson.getOrDefault("expires_in", 5184000)).intValue();

            var userReq = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            var userRes = HTTP.send(userReq, HttpResponse.BodyHandlers.ofString());
            if (userRes.statusCode() != 200) {
                throw new RuntimeException("Error obtenint perfil LinkedIn: " + userRes.body());
            }
            @SuppressWarnings("unchecked")
            var userJson = objectMapper.readValue(userRes.body(), Map.class);
            String sub = (String) userJson.get("sub");
            String name = (String) userJson.getOrDefault("name", "LinkedIn");
            if (sub == null || sub.isBlank()) throw new RuntimeException("Perfil LinkedIn sense identificador");
            String personUrn = "urn:li:person:" + sub;

            var conn = connectionRepo.findByTenantId(tenantId).orElseGet(LinkedInConnection::new);
            conn.setTenantId(tenantId);
            conn.setPersonUrn(personUrn);
            conn.setDisplayName(name);
            conn.setEncryptedAccessToken(vaultEncryption.encrypt(accessToken));
            conn.setTokenExpiresAt(Instant.now().plus(expiresIn, ChronoUnit.SECONDS));
            conn.setActive(true);
            connectionRepo.save(conn);

            log.info("Connexió LinkedIn desada per tenant {} ({})", tenantId, name);
            return new ConnectedResponse(name, personUrn);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al callback de LinkedIn: " + e.getMessage(), e);
        }
    }

    private String required(String key) {
        String v = sysConfig.get(key);
        if (v == null || v.isBlank()) throw new RuntimeException("Falta la configuració " + key);
        return v;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
