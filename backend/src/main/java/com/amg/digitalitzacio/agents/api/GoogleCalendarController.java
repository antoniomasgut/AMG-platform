package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/nexe/tenants/{tenantId}/calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;
    private final NexeServiceConfigService nexeConfigService;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    // ── Fase 1: AMG crea el calendari per al tenant ──────────────

    @PostMapping("/provision")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> provision(@PathVariable UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        if (!calendarService.isConfigured()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "GOOGLE_CALENDAR_SA_JSON no configurat a /portal/admin/config"));
        }

        try {
            String calendarName = "Cites — " + tenant.getName();
            String ownerEmail   = tenant.getEmail();
            String calendarId   = calendarService.createCalendarForTenant(calendarName, ownerEmail);

            // Actualitza (o crea) el config AGENDA amb el nou calendarId i calendar_type=google
            String existingJson = nexeConfigService.get(tenantId, "AGENDA")
                    .map(c -> c.getConfigJson())
                    .orElse("{}");
            Map<String, Object> agenda = objectMapper.readValue(existingJson, new TypeReference<>() {});
            agenda.put("calendar_type", "google");
            agenda.put("google_calendar_id", calendarId);
            nexeConfigService.save(tenantId, "AGENDA", objectMapper.writeValueAsString(agenda));

            return ResponseEntity.ok(Map.of(
                    "calendarId", calendarId,
                    "message", "Calendari creat i compartit amb " + (ownerEmail != null ? ownerEmail : "—")
            ));
        } catch (Exception e) {
            log.error("Error provisionant calendari per tenant {}", tenantId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── SA info & validació ───────────────────────────────────────

    @GetMapping("/sa-email")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> getSaEmail(@PathVariable UUID tenantId) {
        if (!calendarService.isConfigured()) {
            return ResponseEntity.ok(Map.of("email", "", "configured", "false"));
        }
        try {
            String email = calendarService.getServiceAccountEmail();
            return ResponseEntity.ok(Map.of("email", email, "configured", "true"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("email", "", "configured", "false", "error", e.getMessage()));
        }
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Object>> validateCalendarId(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> body) {
        String calendarId = body.get("calendarId");
        if (calendarId == null || calendarId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "calendarId buit"));
        }
        try {
            boolean valid = calendarService.validateCalendarAccess(calendarId);
            return ResponseEntity.ok(Map.of("valid", valid,
                    "message", valid ? "Calendari accessible amb el Service Account d'AMG" : "No s'ha pogut accedir al calendari"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    // ── Fase 2: OAuth — el client autoritza amb el seu propi compte Google ──

    @GetMapping("/oauth-url")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<Map<String, String>> getOAuthUrl(
            @PathVariable UUID tenantId,
            HttpServletRequest request) {

        if (!calendarService.isOAuthConfigured()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "GOOGLE_OAUTH_CLIENT_ID / CLIENT_SECRET no configurats"));
        }

        String redirectUri = buildRedirectUri(request);
        String url = calendarService.buildOAuthUrl(tenantId.toString(), redirectUri);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private String buildRedirectUri(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int port      = request.getServerPort();
        boolean isDefault = ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
        String base = scheme + "://" + host + (isDefault ? "" : ":" + port);
        return base + "/api/v1/nexe/calendar/oauth-callback";
    }
}
