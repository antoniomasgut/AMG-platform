package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.ConversationResponse;
import com.amg.digitalitzacio.agents.api.dto.SendReplyRequest;
import com.amg.digitalitzacio.agents.application.DemoInboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController("demoInboxController")
@RequiredArgsConstructor
public class DemoController {

    private static final String DEMO_EMAIL = "demo@amgdl.com";

    @Value("${app.api.base-url:https://api.amgdl.com}")
    private String apiBaseUrl;

    private final DemoInboxService demoInboxService;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record DemoCreateRequest(String sector, String companyName, String agentContext,
                                    String prospectEmail, String locale) {}

    public record DemoUpdateRequest(String companyName, String agentContext) {}

    public record DemoSessionResponse(
            UUID token,
            String landingUrl,
            String sector,
            String companyName,
            String demoEmail,
            String locale,
            Instant expiresAt,
            boolean active) {}

    public record DemoInboxResponse(
            UUID token,
            String prospectEmail,
            String demoEmail,
            String companyName,
            String agentContext,
            String sector,
            String landingSlug,
            String landingUrl,
            Instant expiresAt,
            boolean blocked,
            String blockReason,
            boolean active,
            List<ConversationResponse> thread) {}

    // ── Admin endpoints (auth required) ──────────────────────────────────────

    @PostMapping("/api/v1/admin/demo/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public DemoSessionResponse createDemoSession(@Valid @RequestBody DemoCreateRequest request) {
        var session = demoInboxService.createSession(
                request.prospectEmail(),
                request.companyName(),
                request.agentContext(),
                request.sector(),
                request.locale());
        return toSessionResponse(session);
    }

    @GetMapping("/api/v1/admin/demo/sessions")
    @PreAuthorize("isAuthenticated()")
    public List<DemoSessionResponse> listDemoSessions() {
        return demoInboxService.listSessions().stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @DeleteMapping("/api/v1/admin/demo/sessions/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deactivateDemoSession(@PathVariable UUID token) {
        demoInboxService.deactivateSession(token);
    }

    @PatchMapping("/api/v1/admin/demo/sessions/{token}")
    @PreAuthorize("isAuthenticated()")
    public DemoSessionResponse updateDemoSession(
            @PathVariable UUID token,
            @Valid @RequestBody DemoUpdateRequest request) {
        var session = demoInboxService.updateSession(token, request.companyName(), request.agentContext());
        return toSessionResponse(session);
    }

    // ── Public endpoints (token-only access) ─────────────────────────────────

    @GetMapping("/api/v1/demo/sessions/{token}")
    public DemoInboxResponse getDemoSession(@PathVariable UUID token) {
        var session = demoInboxService.validateSession(token);
        var tenantId = demoInboxService.getDemoTenantId();
        var thread = demoInboxService.getThread(tenantId, session.getProspectEmail());

        return new DemoInboxResponse(
                session.getToken(),
                session.getProspectEmail(),
                DEMO_EMAIL,
                session.getCompanyName(),
                session.getAgentContext(),
                session.getSector(),
                session.getLandingSlug(),
                buildLandingUrl(session.getLandingSlug(), session.getLocale()),
                session.getExpiresAt(),
                session.getBlockedAt() != null,
                session.getBlockReason(),
                Boolean.TRUE.equals(session.getIsActive()),
                thread);
    }

    @PostMapping("/api/v1/demo/sessions/{token}/reply")
    public ResponseEntity<Map<String, String>> sendDemoReply(
            @PathVariable UUID token,
            @Valid @RequestBody SendReplyRequest request) {
        var session = demoInboxService.validateSession(token);

        if (session.getBlockedAt() != null) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("blocked", "true",
                            "reason", session.getBlockReason() != null ? session.getBlockReason() : ""));
        }

        String blockReason = demoInboxService.sendReply(session, request.text());
        if (blockReason != null) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("blocked", "true", "reason", blockReason));
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ── Legacy inbox endpoints (backwards compat) ────────────────────────────

    @PostMapping("/api/v1/admin/demo/inbox")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public DemoSessionResponse createDemoSessionLegacy(@Valid @RequestBody DemoCreateRequest request) {
        return createDemoSession(request);
    }

    @GetMapping("/api/v1/demo/inbox/{token}")
    public DemoInboxResponse getDemoInboxLegacy(@PathVariable UUID token) {
        return getDemoSession(token);
    }

    @PostMapping("/api/v1/demo/inbox/{token}/reply")
    public ResponseEntity<Map<String, String>> sendDemoReplyLegacy(
            @PathVariable UUID token,
            @Valid @RequestBody SendReplyRequest request) {
        return sendDemoReply(token, request);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DemoSessionResponse toSessionResponse(com.amg.digitalitzacio.agents.domain.DemoSession session) {
        String loc = session.getLocale() != null ? session.getLocale() : "ca";
        return new DemoSessionResponse(
                session.getToken(),
                buildLandingUrl(session.getLandingSlug(), loc),
                session.getSector(),
                session.getCompanyName(),
                DEMO_EMAIL,
                loc,
                session.getExpiresAt(),
                Boolean.TRUE.equals(session.getIsActive()));
    }

    private String buildLandingUrl(String slug, String locale) {
        if (slug == null || slug.isBlank()) return null;
        String loc = locale != null && !locale.isBlank() ? locale : "ca";
        return apiBaseUrl + "/api/v1/engine/render/" + slug + "/" + loc;
    }
}
