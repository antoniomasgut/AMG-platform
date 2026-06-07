package com.amg.digitalitzacio.google.api;

import com.amg.digitalitzacio.google.api.dto.*;
import com.amg.digitalitzacio.google.application.CalendarEventItem;
import com.amg.digitalitzacio.google.application.DriveFileItem;
import com.amg.digitalitzacio.google.application.GoogleAuthService;
import com.amg.digitalitzacio.google.application.GoogleOrchestrator;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/google")
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleAuthService authService;
    private final GoogleOrchestrator orchestrator;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<GoogleStatusResponse> getStatus(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(orchestrator.getStatus(tenantId));
    }

    @PostMapping("/auth-url")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AuthUrlResponse> getAuthUrl(
            @PathVariable UUID tenantId,
            @RequestBody AuthUrlRequest req) {
        var result = authService.generateAuthUrl(tenantId, req.modules(), req.redirectUri());
        return ResponseEntity.ok(new AuthUrlResponse(result.authUrl(), result.stateToken()));
    }

    @PutMapping("/modules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> updateModules(
            @PathVariable UUID tenantId,
            @RequestBody ModuleConfigRequest req) {
        orchestrator.updateModules(tenantId, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<String> testConnection(@PathVariable UUID tenantId) {
        try {
            var status = orchestrator.getStatus(tenantId);
            return ResponseEntity.ok("Connexió OK: " + status.email());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/disconnect")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> disconnect(@PathVariable UUID tenantId) {
        orchestrator.disconnect(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<String> sendMail(
            @PathVariable UUID tenantId,
            @RequestBody SendMailRequest req) {
        try {
            orchestrator.sendMail(tenantId, req);
            return ResponseEntity.ok("Correu enviat");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/drive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<DriveFileItem>> listDriveFiles(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String folderId) {
        return ResponseEntity.ok(orchestrator.listDriveFiles(tenantId, folderId));
    }

    @GetMapping("/calendar/events")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<CalendarEventItem>> listCalendarEvents(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(orchestrator.listCalendarEvents(tenantId));
    }
}
