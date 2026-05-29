package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.ConversationResponse;
import com.amg.digitalitzacio.agents.api.dto.SendReplyRequest;
import com.amg.digitalitzacio.agents.application.DemoInboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController("demoInboxController")
@RequiredArgsConstructor
public class DemoController {

    private static final String DEMO_EMAIL = "demo@amgdl.com";
    private static final String INBOX_BASE_URL = "https://amgdl.com/demo/inbox/";

    private final DemoInboxService demoInboxService;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record DemoCreateRequest(String prospectEmail, String companyName, String agentContext) {}

    public record DemoUpdateRequest(String companyName, String agentContext) {}

    public record DemoSessionResponse(UUID token, String url, String demoEmail, Instant expiresAt) {}

    public record DemoInboxResponse(
            UUID token,
            String prospectEmail,
            String demoEmail,
            String companyName,
            String agentContext,
            Instant expiresAt,
            boolean blocked,
            String blockReason,
            List<ConversationResponse> thread) {}

    // ── Admin endpoints (auth required) ──────────────────────────────────────

    @PostMapping("/api/v1/admin/demo/inbox")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public DemoSessionResponse createDemoSession(@RequestBody DemoCreateRequest request) {
        var session = demoInboxService.createSession(
                request.prospectEmail(),
                request.companyName(),
                request.agentContext());
        return new DemoSessionResponse(
                session.getToken(),
                INBOX_BASE_URL + session.getToken(),
                DEMO_EMAIL,
                session.getExpiresAt());
    }

    @PatchMapping("/api/v1/admin/demo/inbox/{token}")
    @PreAuthorize("isAuthenticated()")
    public DemoSessionResponse updateDemoSession(
            @PathVariable UUID token,
            @RequestBody DemoUpdateRequest request) {
        var session = demoInboxService.updateSession(token, request.companyName(), request.agentContext());
        return new DemoSessionResponse(
                session.getToken(),
                INBOX_BASE_URL + session.getToken(),
                DEMO_EMAIL,
                session.getExpiresAt());
    }

    // ── Public endpoints (token-only access) ─────────────────────────────────

    @GetMapping("/api/v1/demo/inbox/{token}")
    public DemoInboxResponse getDemoInbox(@PathVariable UUID token) {
        var session = demoInboxService.validateSession(token);
        var tenantId = demoInboxService.getDemoTenantId();
        var thread = demoInboxService.getThread(tenantId, session.getProspectEmail());

        return new DemoInboxResponse(
                session.getToken(),
                session.getProspectEmail(),
                DEMO_EMAIL,
                session.getCompanyName(),
                session.getAgentContext(),
                session.getExpiresAt(),
                session.getBlockedAt() != null,
                session.getBlockReason(),
                thread);
    }

    @PostMapping("/api/v1/demo/inbox/{token}/reply")
    public ResponseEntity<Map<String, String>> sendDemoReply(
            @PathVariable UUID token,
            @RequestBody SendReplyRequest request) {
        var session = demoInboxService.validateSession(token);

        // Blocked sessions cannot receive more messages
        if (session.getBlockedAt() != null) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("blocked", "true", "reason", session.getBlockReason() != null ? session.getBlockReason() : ""));
        }

        String blockReason = demoInboxService.sendReply(session, request.text());
        if (blockReason != null) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("blocked", "true", "reason", blockReason));
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
