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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String TOKEN_URL   = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3";

    // ── SA configurada? ──────────────────────────────────────────

    public boolean isConfigured() {
        String json = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        return json != null && !json.isBlank();
    }

    public boolean isOAuthConfigured() {
        String clientId     = systemConfigService.get("GOOGLE_OAUTH_CLIENT_ID");
        String clientSecret = systemConfigService.get("GOOGLE_OAUTH_CLIENT_SECRET");
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    // ── Fase 1: SA crea el calendari per al tenant ───────────────

    /**
     * Crea un nou Google Calendar amb nom {@code calendarName} i el comparteix
     * en mode escriptura amb {@code ownerEmail}. Retorna el calendarId.
     */
    public String createCalendarForTenant(String calendarName, String ownerEmail) throws Exception {
        String accessToken = getSaAccessToken();

        // 1. Crear el calendari
        String body = objectMapper.writeValueAsString(Map.of("summary", calendarName));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(CALENDAR_API + "/calendars"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Error creant calendari: " + res.statusCode() + " " + res.body());
        }
        Map<String, Object> cal = objectMapper.readValue(res.body(), new TypeReference<>() {});
        String calendarId = (String) cal.get("id");

        // 2. Compartir amb el propietari del tenant (permís escriptura)
        if (ownerEmail != null && !ownerEmail.isBlank()) {
            shareCalendar(calendarId, ownerEmail, accessToken);
        }

        log.info("Google Calendar creat: id={}, name={}, sharedWith={}", calendarId, calendarName, ownerEmail);
        return calendarId;
    }

    private void shareCalendar(String calendarId, String email, String accessToken) throws Exception {
        String aclBody = objectMapper.writeValueAsString(Map.of(
                "role", "writer",
                "scope", Map.of("type", "user", "value", email)
        ));
        String encodedId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(CALENDAR_API + "/calendars/" + encodedId + "/acl"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(aclBody))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.warn("No s'ha pogut compartir el calendari amb {}: {} {}", email, res.statusCode(), res.body());
        }
    }

    public String getServiceAccountEmail() throws Exception {
        String saJson = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        if (saJson == null || saJson.isBlank()) throw new IllegalStateException("GOOGLE_CALENDAR_SA_JSON no configurat");
        Map<String, Object> sa = objectMapper.readValue(saJson, new TypeReference<>() {});
        return (String) sa.get("client_email");
    }

    /**
     * Comprova que el SA té accés al calendari intentant llegir-ne els metadades.
     * Retorna true si el calendari és accessible, false si no.
     */
    public boolean validateCalendarAccess(String calendarId) throws Exception {
        String accessToken = getSaAccessToken();
        String encodedId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(CALENDAR_API + "/calendars/" + encodedId))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return true;
        if (res.statusCode() == 404 || res.statusCode() == 403) return false;
        throw new IllegalStateException("Error validant calendari: " + res.statusCode() + " " + res.body());
    }

    // ── Fase 2: OAuth per compte del client ──────────────────────

    /** Genera la URL d'autorització OAuth 2.0 per al client. */
    public String buildOAuthUrl(String tenantId, String redirectUri) {
        String clientId = systemConfigService.get("GOOGLE_OAUTH_CLIENT_ID");
        return "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
            + "&response_type=code"
            + "&scope=" + URLEncoder.encode(CALENDAR_SCOPE, StandardCharsets.UTF_8)
            + "&access_type=offline"
            + "&prompt=consent"
            + "&state=" + tenantId;
    }

    /** Intercanvia el codi per un refresh token. Retorna el refresh_token. */
    public String exchangeCodeForRefreshToken(String code, String redirectUri) throws Exception {
        String clientId     = systemConfigService.get("GOOGLE_OAUTH_CLIENT_ID");
        String clientSecret = systemConfigService.get("GOOGLE_OAUTH_CLIENT_SECRET");

        String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        var req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = objectMapper.readValue(res.body(), new TypeReference<>() {});
        String refreshToken = (String) resp.get("refresh_token");
        if (refreshToken == null) throw new IllegalStateException("No refresh_token: " + res.body());
        return refreshToken;
    }

    /** Crea un event usant el refresh token OAuth del tenant. */
    public void createEventOAuth(String refreshToken, String calendarId,
                                  String title, LocalDateTime start,
                                  int durationMinutes, String description) {
        if (refreshToken == null || refreshToken.isBlank() || calendarId == null) return;
        try {
            String accessToken = getOAuthAccessToken(refreshToken);
            createEventWithToken(accessToken, calendarId, title, start, durationMinutes, description);
        } catch (Exception e) {
            log.error("Error creant event via OAuth per calendarId={}", calendarId, e);
        }
    }

    private String getOAuthAccessToken(String refreshToken) throws Exception {
        String clientId     = systemConfigService.get("GOOGLE_OAUTH_CLIENT_ID");
        String clientSecret = systemConfigService.get("GOOGLE_OAUTH_CLIENT_SECRET");

        String body = "grant_type=refresh_token"
                + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        var req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = objectMapper.readValue(res.body(), new TypeReference<>() {});
        String token = (String) resp.get("access_token");
        if (token == null) throw new IllegalStateException("No access_token OAuth: " + res.body());
        return token;
    }

    // ── Crear event (comú SA + OAuth) ────────────────────────────

    /** Crea un event al calendari amb el Service Account d'AMG. */
    public void createEvent(String calendarId, String title,
                             LocalDateTime start, int durationMinutes,
                             String description) {
        if (calendarId == null || calendarId.isBlank()) return;
        try {
            String accessToken = getSaAccessToken();
            createEventWithToken(accessToken, calendarId, title, start, durationMinutes, description);
        } catch (Exception e) {
            log.error("Failed to create Google Calendar event for calendarId={}", calendarId, e);
        }
    }

    private void createEventWithToken(String accessToken, String calendarId,
                                       String title, LocalDateTime start,
                                       int durationMinutes, String description) throws Exception {
        createEventWithTokenReturningLink(accessToken, calendarId, title, start, durationMinutes, description, false);
    }

    /** Crea un event amb Google Meet i retorna el link de Meet (null si falla). */
    public String createEventWithMeet(String calendarId, String title,
                                       LocalDateTime start, int durationMinutes,
                                       String description, String attendeeEmail) {
        if (calendarId == null || calendarId.isBlank()) return null;
        try {
            String accessToken = getSaAccessToken();
            return createEventWithTokenReturningLink(accessToken, calendarId, title, start, durationMinutes, description, true);
        } catch (Exception e) {
            log.error("Failed to create Meet event for calendarId={}", calendarId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String createEventWithTokenReturningLink(String accessToken, String calendarId,
                                                      String title, LocalDateTime start,
                                                      int durationMinutes, String description,
                                                      boolean withMeet) throws Exception {
        LocalDateTime end = start.plusMinutes(durationMinutes);
        String eventJson = withMeet
                ? buildEventJsonWithMeet(title, start, end, description)
                : buildEventJson(title, start, end, description);
        String encodedId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
        String url = CALENDAR_API + "/calendars/" + encodedId + "/events"
                + (withMeet ? "?conferenceDataVersion=1" : "");

        var req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventJson))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            log.info("Google Calendar event created: calendarId={}, title={}, meet={}", calendarId, title, withMeet);
            if (withMeet) {
                Map<String, Object> body = objectMapper.readValue(res.body(), new TypeReference<>() {});
                var confData = (Map<String, Object>) body.get("conferenceData");
                if (confData != null) {
                    var entries = (List<Map<String, Object>>) confData.get("entryPoints");
                    if (entries != null) {
                        return entries.stream()
                                .filter(e -> "video".equals(e.get("entryPointType")))
                                .map(e -> (String) e.get("uri"))
                                .findFirst().orElse(null);
                    }
                }
            }
        } else {
            log.error("Google Calendar API error {}: {}", res.statusCode(), res.body());
        }
        return null;
    }

    /** Consulta els períodes ocupats via Freebusy API. */
    @SuppressWarnings("unchecked")
    public List<Instant[]> getFreeBusySlots(String calendarId, Instant from, Instant to) throws Exception {
        String accessToken = getSaAccessToken();
        String body = objectMapper.writeValueAsString(Map.of(
                "timeMin", from.toString(),
                "timeMax", to.toString(),
                "items", List.of(Map.of("id", calendarId))
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(CALENDAR_API + "/freeBusy"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) return List.of();

        Map<String, Object> resp = objectMapper.readValue(res.body(), new TypeReference<>() {});
        var calendars = (Map<String, Map<String, Object>>) resp.get("calendars");
        if (calendars == null || !calendars.containsKey(calendarId)) return List.of();
        var busyList = (List<Map<String, String>>) calendars.get(calendarId).get("busy");
        if (busyList == null) return List.of();

        return busyList.stream().map(b -> new Instant[]{
                Instant.parse(b.get("start")),
                Instant.parse(b.get("end"))
        }).toList();
    }

    // ── SA token ─────────────────────────────────────────────────

    private String getSaAccessToken() throws Exception {
        String saJson = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        if (saJson == null || saJson.isBlank()) throw new IllegalStateException("GOOGLE_CALENDAR_SA_JSON not configured");
        Map<String, Object> sa = objectMapper.readValue(saJson, new TypeReference<>() {});
        return getAccessToken((String) sa.get("client_email"), (String) sa.get("private_key"));
    }

    private String getAccessToken(String clientEmail, String privateKeyPem) throws Exception {
        long now = Instant.now().getEpochSecond();

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String claimsJson = objectMapper.writeValueAsString(Map.of(
                "iss", clientEmail, "scope", CALENDAR_SCOPE,
                "aud", TOKEN_URL,   "exp", now + 3600, "iat", now));
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
        if (token == null) throw new IllegalStateException("No access_token SA: " + res.body());
        return token;
    }

    private byte[] signRS256(byte[] data, String pem) throws Exception {
        String key = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        PrivateKey pk = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key)));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pk);
        sig.update(data);
        return sig.sign();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String buildEventJson(String title, LocalDateTime start, LocalDateTime end, String description) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return """
                {
                  "summary": %s,
                  "description": %s,
                  "start": {"dateTime": "%s", "timeZone": "Europe/Madrid"},
                  "end":   {"dateTime": "%s", "timeZone": "Europe/Madrid"}
                }""".formatted(jsonString(title), jsonString(description != null ? description : ""),
                start.format(fmt), end.format(fmt));
    }

    private String buildEventJsonWithMeet(String title, LocalDateTime start, LocalDateTime end, String description) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String requestId = UUID.randomUUID().toString();
        return """
                {
                  "summary": %s,
                  "description": %s,
                  "start": {"dateTime": "%s", "timeZone": "Europe/Madrid"},
                  "end":   {"dateTime": "%s", "timeZone": "Europe/Madrid"},
                  "conferenceData": {
                    "createRequest": {
                      "requestId": "%s",
                      "conferenceSolutionKey": {"type": "hangoutsMeet"}
                    }
                  }
                }""".formatted(jsonString(title), jsonString(description != null ? description : ""),
                start.format(fmt), end.format(fmt), requestId);
    }

    private String jsonString(String s) {
        try { return objectMapper.writeValueAsString(s); }
        catch (Exception e) { return "\"" + s.replace("\"", "'") + "\""; }
    }

    // ── Drive AMG via SA ─────────────────────────────────────────

    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
    private static final String DRIVE_API   = "https://www.googleapis.com/drive/v3";
    private static final String DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3";

    public record DriveDocResult(String fileId, String webViewLink) {}

    /**
     * Exporta HTML al Drive d'AMG dins Tenants/{tenantName}/.
     * Crea les carpetes si no existeixen i desa l'ID arrel a system_settings.
     */
    public DriveDocResult exportHtmlToDriveViaSA(String html, String title, String tenantName) throws Exception {
        String accessToken = getSaAccessTokenWithScope(DRIVE_SCOPE);

        String rootFolderId = getOrCreateTenantsRootFolder(accessToken);
        String tenantFolderId = getOrCreateFolder(accessToken, tenantName, rootFolderId);

        return uploadHtmlAsGoogleDoc(accessToken, html, title, tenantFolderId);
    }

    private String getSaAccessTokenWithScope(String scope) throws Exception {
        String saJson = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        if (saJson == null || saJson.isBlank()) throw new IllegalStateException("GOOGLE_CALENDAR_SA_JSON no configurat");

        Map<String, Object> sa = objectMapper.readValue(saJson, new TypeReference<>() {});
        String clientEmail = (String) sa.get("client_email");
        String privateKeyPem = (String) sa.get("private_key");

        long now = Instant.now().getEpochSecond();
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"iss\":\"" + clientEmail + "\","
                        + "\"scope\":\"" + scope + "\","
                        + "\"aud\":\"https://oauth2.googleapis.com/token\","
                        + "\"exp\":" + (now + 3600) + ","
                        + "\"iat\":" + now + "}").getBytes());

        String toSign = header + "." + payload;
        String keyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(toSign.getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

        String jwt = toSign + "." + signature;
        String body = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);

        var req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = objectMapper.readValue(res.body(), new TypeReference<>() {});
        String token = (String) resp.get("access_token");
        if (token == null) throw new IllegalStateException("SA Drive token error: " + res.body());
        return token;
    }

    private String getOrCreateTenantsRootFolder(String accessToken) throws Exception {
        String cached = systemConfigService.get("GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID");
        if (cached != null && !cached.isBlank()) {
            if (folderExists(accessToken, cached)) return cached;
        }
        String folderId = getOrCreateFolder(accessToken, "Tenants", null);
        systemConfigService.setInternal("GOOGLE_DRIVE_AMG_TENANTS_FOLDER_ID", folderId);
        return folderId;
    }

    private boolean folderExists(String accessToken, String folderId) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(DRIVE_API + "/files/" + folderId + "?fields=id,trashed"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return false;
            Map<String, Object> f = objectMapper.readValue(res.body(), new TypeReference<>() {});
            return !Boolean.TRUE.equals(f.get("trashed"));
        } catch (Exception e) {
            return false;
        }
    }

    private String getOrCreateFolder(String accessToken, String name, String parentId) throws Exception {
        // Cerca carpeta existent
        String query = "name='" + name.replace("'", "\\'") + "'"
                + " and mimeType='application/vnd.google-apps.folder'"
                + " and trashed=false"
                + (parentId != null ? " and '" + parentId + "' in parents" : "");
        var searchReq = HttpRequest.newBuilder()
                .uri(URI.create(DRIVE_API + "/files?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&fields=files(id)"))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();
        var searchRes = httpClient.send(searchReq, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> searchJson = objectMapper.readValue(searchRes.body(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        var files = (java.util.List<Map<String, Object>>) searchJson.get("files");
        if (files != null && !files.isEmpty()) {
            return (String) files.get(0).get("id");
        }

        // Crea carpeta nova
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("name", name);
        meta.put("mimeType", "application/vnd.google-apps.folder");
        if (parentId != null) meta.put("parents", List.of(parentId));

        var createReq = HttpRequest.newBuilder()
                .uri(URI.create(DRIVE_API + "/files?fields=id"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(meta)))
                .build();
        var createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        if (createRes.statusCode() < 200 || createRes.statusCode() >= 300) {
            throw new IllegalStateException("Error creant carpeta '" + name + "': " + createRes.statusCode());
        }
        Map<String, Object> created = objectMapper.readValue(createRes.body(), new TypeReference<>() {});
        return (String) created.get("id");
    }

    private DriveDocResult uploadHtmlAsGoogleDoc(String accessToken, String html, String title, String folderId) throws Exception {
        String boundary = "amg_sa_boundary_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String metadata = objectMapper.writeValueAsString(Map.of(
                "name", title,
                "mimeType", "application/vnd.google-apps.document",
                "parents", List.of(folderId)
        ));
        String body = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n\r\n"
                + html + "\r\n"
                + "--" + boundary + "--";

        var req = HttpRequest.newBuilder()
                .uri(URI.create(DRIVE_UPLOAD + "/files?uploadType=multipart&fields=id,webViewLink"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Drive upload SA failed: " + res.statusCode() + " " + res.body());
        }
        Map<String, Object> json = objectMapper.readValue(res.body(), new TypeReference<>() {});
        return new DriveDocResult(
                (String) json.get("id"),
                json.getOrDefault("webViewLink", "").toString()
        );
    }
}
