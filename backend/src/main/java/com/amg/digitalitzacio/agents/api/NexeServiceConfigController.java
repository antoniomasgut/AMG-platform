package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nexe/tenants/{tenantId}/configs")
@RequiredArgsConstructor
public class NexeServiceConfigController {

    private final NexeServiceConfigService service;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Map<String, String>> getAll(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getAllAsMap(tenantId));
    }

    @GetMapping("/{serviceKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<String> get(
            @PathVariable UUID tenantId,
            @PathVariable String serviceKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.get(tenantId, serviceKey)
                .map(c -> ResponseEntity.ok(c.getConfigJson()))
                .orElse(ResponseEntity.ok("{}"));
    }

    @PutMapping("/{serviceKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> save(
            @PathVariable UUID tenantId,
            @PathVariable String serviceKey,
            @RequestBody String configJson,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.save(tenantId, serviceKey, configJson);
        return ResponseEntity.ok().build();
    }
}
