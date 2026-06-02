package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint fix de callback OAuth de Google Calendar.
 * URL a registrar a Google Cloud Console: https://api.amgdl.com/api/v1/nexe/calendar/oauth-callback
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/nexe/calendar")
@RequiredArgsConstructor
public class GoogleCalendarOAuthCallbackController {

    private final GoogleCalendarService calendarService;
    private final NexeServiceConfigService nexeConfigService;
    private final ObjectMapper objectMapper;

    @GetMapping("/oauth-callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request) {

        try {
            UUID tenantId   = UUID.fromString(state);
            String redirectUri = buildRedirectUri(request);
            String refreshToken = calendarService.exchangeCodeForRefreshToken(code, redirectUri);

            String existingJson = nexeConfigService.get(tenantId, "AGENDA")
                    .map(c -> c.getConfigJson())
                    .orElse("{}");
            Map<String, Object> agenda = objectMapper.readValue(existingJson, new TypeReference<>() {});
            agenda.put("calendar_type", "google_oauth");
            agenda.put("google_calendar_id", "primary");
            agenda.put("google_refresh_token", refreshToken);
            nexeConfigService.save(tenantId, "AGENDA", objectMapper.writeValueAsString(agenda));

            log.info("Google Calendar OAuth configurat per tenant {}", tenantId);
            return ResponseEntity.status(302)
                    .location(URI.create("/portal/admin/tenants/" + tenantId + "/nexe/agenda?oauth=success"))
                    .build();
        } catch (Exception e) {
            log.error("Error al callback OAuth Google Calendar", e);
            return ResponseEntity.status(302)
                    .location(URI.create("/portal/admin?error=calendar_oauth"))
                    .build();
        }
    }

    private String buildRedirectUri(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int port      = request.getServerPort();
        boolean isDefault = ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
        return scheme + "://" + host + (isDefault ? "" : ":" + port) + "/api/v1/nexe/calendar/oauth-callback";
    }
}
