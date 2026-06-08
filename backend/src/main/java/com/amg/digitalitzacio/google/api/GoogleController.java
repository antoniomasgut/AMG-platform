package com.amg.digitalitzacio.google.api;

import com.amg.digitalitzacio.google.api.dto.*;
import com.amg.digitalitzacio.google.application.CalendarEventItem;
import com.amg.digitalitzacio.google.application.DriveFileItem;
import com.amg.digitalitzacio.google.application.GoogleAuthService;
import com.amg.digitalitzacio.google.application.GoogleOrchestrator;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/google")
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleAuthService authService;
    private final GoogleOrchestrator orchestrator;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<GoogleStatusResponse> getStatus(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orchestrator.getStatus(tenantId));
    }

    @PostMapping("/auth-url")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<AuthUrlResponse> getAuthUrl(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AuthUrlRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = authService.generateAuthUrl(tenantId, req.modules(), req.redirectUri());
        return ResponseEntity.ok(new AuthUrlResponse(result.authUrl(), result.stateToken()));
    }

    @PutMapping("/modules")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> updateModules(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ModuleConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        orchestrator.updateModules(tenantId, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<String> testConnection(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            var status = orchestrator.getStatus(tenantId);
            return ResponseEntity.ok("Connexió OK: " + status.email());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/disconnect")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> disconnect(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orchestrator.disconnect(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<String> sendMail(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SendMailRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            orchestrator.sendMail(tenantId, req);
            return ResponseEntity.ok("Correu enviat");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/drive")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<List<DriveFileItem>> listDriveFiles(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String folderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orchestrator.listDriveFiles(tenantId, folderId));
    }

    @GetMapping("/calendar/events")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<List<CalendarEventItem>> listCalendarEvents(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orchestrator.listCalendarEvents(tenantId));
    }
}
