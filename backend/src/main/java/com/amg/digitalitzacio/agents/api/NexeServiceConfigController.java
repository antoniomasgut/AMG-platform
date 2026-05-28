package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nexe/tenants/{tenantId}/configs")
@RequiredArgsConstructor
public class NexeServiceConfigController {

    private final NexeServiceConfigService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> getAll(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(service.getAllAsMap(tenantId));
    }

    @GetMapping("/{serviceKey}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<String> get(@PathVariable UUID tenantId, @PathVariable String serviceKey) {
        return service.get(tenantId, serviceKey)
                .map(c -> ResponseEntity.ok(c.getConfigJson()))
                .orElse(ResponseEntity.ok("{}"));
    }

    @PutMapping("/{serviceKey}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> save(
            @PathVariable UUID tenantId,
            @PathVariable String serviceKey,
            @RequestBody String configJson) {
        service.save(tenantId, serviceKey, configJson);
        return ResponseEntity.ok().build();
    }
}
