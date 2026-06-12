package com.amg.digitalitzacio.legal.api;

import com.amg.digitalitzacio.legal.application.DpaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DpaController {

    private final DpaService dpaService;

    // ── Autenticat (ADMIN / SUPER_ADMIN) ─────────────────────────

    @GetMapping("/api/v1/legal/tenants/{tenantId}/dpa")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DpaService.DpaStatusResponse> getStatus(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(dpaService.getStatus(tenantId));
    }

    @PostMapping("/api/v1/legal/tenants/{tenantId}/dpa/send")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DpaService.DpaStatusResponse> send(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(dpaService.sendRequest(tenantId));
    }

    // ── Públic (token) ────────────────────────────────────────────

    @GetMapping("/api/v1/public/dpa/{token}")
    public ResponseEntity<DpaService.DpaPreviewResponse> getDocument(@PathVariable String token) {
        try {
            return ResponseEntity.ok(dpaService.getDocument(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/v1/public/dpa/{token}/sign")
    public ResponseEntity<String> sign(
            @PathVariable String token,
            @RequestBody DpaService.SignRequest req,
            HttpServletRequest request) {
        try {
            String ip = resolveIp(request);
            String ua = request.getHeader("User-Agent");
            dpaService.sign(token, req, ip, ua);
            return ResponseEntity.ok("Signat correctament");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
